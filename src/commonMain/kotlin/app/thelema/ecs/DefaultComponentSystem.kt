/*
 * Copyright 2020-2021 Anton Trushkov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.thelema.ecs

import app.thelema.action.IAction
import app.thelema.g3d.IArmature
import app.thelema.g3d.IScene
import app.thelema.g3d.cam.ActiveCamera
import app.thelema.g3d.mesh.MeshBuilder
import app.thelema.g3d.ITransformNode
import app.thelema.g3d.particles.IParticleEmitter
import app.thelema.g3d.particles.IParticleSystem
import app.thelema.gl.IMesh
import app.thelema.phys.IRigidBodyPhysicsWorld
import app.thelema.utils.iterate

class DefaultComponentSystem: IComponentSystem, RebuildListener {
    private val armatures = ArrayList<IArmature>()
    private val meshes = ArrayList<IMesh>()
    private val nodes = ArrayList<ITransformNode>()
    private val scenes = ArrayList<IScene>(1)
    private val physicsWorlds = ArrayList<IRigidBodyPhysicsWorld>(1)
    private val actions = ArrayList<IAction>()
    private val mainLoops = ArrayList<MainLoop>()
    private val particleEmitters = ArrayList<IParticleEmitter>()
    private val particleSystemsSet = HashSet<IParticleSystem>()
    private val particleSystems = ArrayList<IParticleSystem>()

    private val componentsCanBeRebuild = ArrayList<ComponentCanBeRebuild>()
    private val updatableComponents = ArrayList<UpdatableComponent>()

    private var idCounter = 1
    private val syncMap = HashMap<Int, Any>()

    private val entityListener = object : EntityListener {
        override fun addedComponent(component: IEntityComponent) {
            if (component is IScene) scenes.add(component)
            if (component is IRigidBodyPhysicsWorld) physicsWorlds.add(component)
            if (component is IAction) actions.add(component)
        }

        override fun removedComponent(component: IEntityComponent) {
            if (component is IScene) scenes.remove(component)
            if (component is IRigidBodyPhysicsWorld) physicsWorlds.remove(component)
            if (component is IAction) actions.remove(component)
        }

        override fun addedComponentToBranch(component: IEntityComponent) {
            if (component is IArmature) armatures.add(component)
            if (component is IMesh) meshes.add(component)
            if (component is ITransformNode) nodes.add(component)
            if (component is ComponentCanBeRebuild && component.rebuildComponentRequested) {
                componentsCanBeRebuild.add(component)
            }
            if (component is UpdatableComponent) updatableComponents.add(component)
            if (component is MainLoop) mainLoops.add(component)
            if (component is IParticleEmitter) particleEmitters.add(component)
        }

        override fun removedComponentFromBranch(component: IEntityComponent) {
            if (component is IArmature) armatures.remove(component)
            if (component is IMesh) meshes.remove(component)
            if (component is ITransformNode) nodes.remove(component)
            if (component is ComponentCanBeRebuild && component.rebuildComponentRequested) {
                componentsCanBeRebuild.remove(component)
            }
            if (component is UpdatableComponent) updatableComponents.remove(component)
            if (component is MainLoop) mainLoops.remove(component)
            if (component is IParticleEmitter) particleEmitters.remove(component)
        }
    }

    override fun requestRebuild(component: ComponentCanBeRebuild) {
        componentsCanBeRebuild.add(component)
    }

    override fun cancelRebuild(component: ComponentCanBeRebuild) {
        componentsCanBeRebuild.remove(component)
    }

    override fun addedScene(entity: IEntity) {
        entity.forEachComponent { entityListener.addedComponent(it) }
        entity.forEachComponentInBranch { entityListener.addedComponentToBranch(it) }
        entity.addEntityListener(entityListener)
        particleEmitters.iterate {
            if (it.entityOrNull?.getRootEntity() == entity) it.isPlaying = true
        }
    }

    override fun removedScene(entity: IEntity) {
        particleEmitters.iterate {
            if (it.entityOrNull?.getRootEntity() == entity) it.isPlaying = false
        }
        entity.forEachComponent { entityListener.removedComponent(it) }
        entity.forEachComponentInBranch { entityListener.removedComponentFromBranch(it) }
        entity.removeEntityListener(entityListener)
    }

    fun newSyncId(sync: Any): Int {
        val id = idCounter
        syncMap[id] = sync
        idCounter++
        return id
    }

    override fun render(shaderChannel: String?) {
        if (shaderChannel == null) {
            scenes.iterate { it.render() }
        } else {
            scenes.iterate { it.render(shaderChannel) }
        }
    }

    override fun update(delta: Float) {
        ActiveCamera.updatePreviousTransform()

        nodes.iterate { it.updatePreviousMatrix() }
        armatures.iterate { it.updatePreviousBoneMatrices() }

        actions.iterate { it.update(delta) }
        mainLoops.iterate { it.update(delta) }

        armatures.iterate { it.preUpdateBoneMatrices() }
        nodes.iterate { if (it.isTransformUpdateRequested) it.updateTransform() }

        updatableComponents.iterate { it.updateComponent(delta) }

        if (ActiveCamera.isCameraUpdateRequested) ActiveCamera.updateCamera()

        particleSystemsSet.clear()
        particleSystems.clear()
        particleEmitters.iterate { emitter ->
            emitter.particleSystem?.also { if (particleSystemsSet.add(it)) particleSystems.add(it) }
            emitter.updateParticles(delta)
        }
        particleSystems.iterate { it.updateParticleSystem(delta) }

        armatures.iterate { it.updateBoneMatrices() }

        if (componentsCanBeRebuild.isNotEmpty()) {
            componentsCanBeRebuild.iterate { it.rebuildComponent() }
            componentsCanBeRebuild.clear()
        }

        physicsWorlds.iterate { it.step(delta) }
    }
}
