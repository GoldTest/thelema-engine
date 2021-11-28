package app.thelema.studio.widget.component

import app.thelema.data.DATA
import app.thelema.ecs.DefaultComponentSystem
import app.thelema.ecs.Entity
import app.thelema.g2d.Sprite
import app.thelema.g3d.IMaterial
import app.thelema.g3d.cam.Camera
import app.thelema.g3d.light.directionalLight
import app.thelema.g3d.mesh.sphereMesh
import app.thelema.g3d.scene
import app.thelema.g3d.simpleSkybox
import app.thelema.gl.GL
import app.thelema.img.*
import app.thelema.math.Rectangle
import app.thelema.math.Vec3
import app.thelema.res.AID
import app.thelema.res.RES
import app.thelema.res.load
import app.thelema.ui.DSKIN
import app.thelema.ui.Scaling
import app.thelema.ui.UIImage
import app.thelema.utils.Color

class MaterialPanel: ComponentPanel<IMaterial>(IMaterial::class) {
    val sprite = Sprite()

    override var component: IMaterial?
        get() = super.component
        set(value) {
            super.component = value
            MaterialPreview.sphere.mesh.material = value
            sprite.texture = MaterialPreview.fb.texture as ITexture2D
        }

    val image = UIImage(sprite)

    var renderPreview = false

    init {
        image.scaling = Scaling.fit
        content.add(image).growX().height(200f).newRow()

        GL.render {
            if (renderPreview) {
                MaterialPreview.fb.render {
                    MaterialPreview.system.render()
                }
            }
        }
    }

    override fun act(delta: Float) {
        renderPreview = true
        MaterialPreview.system.update(delta)
        MaterialPreview.camera.also {
            if (it.viewportWidth != image.imageWidth || it.viewportHeight != image.imageHeight) {
                it.viewportWidth = image.imageWidth
                it.viewportHeight = image.imageHeight
                it.updateCamera()
            }
        }

        super.act(delta)
    }
}

object MaterialPreview {
    val fb = SimpleFrameBuffer(width = 512, height = 512)

    val system = DefaultComponentSystem()

    val camera = Camera {
        lookAt(Vec3(0f, 0f, 2.5f), Vec3(0f))
    }
    val previewScene = Entity("preview") {
        scene {
            activeCamera = camera
        }
        entity("light") {
            directionalLight {
                setDirectionFromPosition(0f, 0f, 1f)
            }
        }
    }
    val sphere = previewScene.entity("sphere").sphereMesh {
        builder.uvName = "TEXCOORD_0"
        setSize(1f)
    }
    val defaultSkyboxTexture = TextureCube {
        GL.call {
            val bytes = DATA.bytes(4 * 4 * 4) {
                val b = 0x000000FF
                val f = 0x808080FF.toInt()
                putRGBAs(
                    b, b, b, b,
                    b, f, f, b,
                    b, f, f, b,
                    b, b, b, b
                )
                rewind()
            }
            sides.forEach {
                it.load(4, 4, bytes, 0)
            }
            bytes.destroy()
        }
    }
    val skybox = previewScene.entity("skybox").simpleSkybox {
        texture = defaultSkyboxTexture
    }

    init {
        system.addedScene(previewScene)
    }
}