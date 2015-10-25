package com.jme3x.jfx.injfx;

import java.net.URL;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.util.TangentBinormalGenerator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class TestDisplayInImageView extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		final FXMLLoader fxmlLoader = new FXMLLoader();
		final URL location = Thread.currentThread().getContextClassLoader().getResource(this.getClass().getCanonicalName().replace('.', '/')+".fxml");
		fxmlLoader.setLocation(location);
		//final ResourceBundle defaultRessources = fxmlLoader.getResources();
		//fxmlLoader.setResources(this.addCustomRessources(defaultRessources));
		fxmlLoader.setBuilderFactory(new JavaFXBuilderFactory());
		final Region root = fxmlLoader.load(location.openStream());
		Controller controller = fxmlLoader.getController();

		JmeForImageView jme = new JmeForImageView();
		jme.bind(controller.image);

		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
		      public void handle(WindowEvent e){
				jme.stop(true);
		      }
		});

		bindOtherControls(jme, controller);
		jme.enqueue(TestDisplayInImageView::createScene);
		jme.enqueue((jmeApp)->{
			jmeApp.getStateManager().attach(new HelloPicking(controller.image));
			
			jmeApp.getStateManager().attach(new CameraDriverAppState());

			//imagePanel.setFocusable(true);
			//imagePanel.requestFocusInWindow();
			CameraDriverInput driver = new CameraDriverInput();
			driver.jme = jmeApp;
			driver.speed = 1.0f;
			CameraDriverInput.bindDefaults(controller.image, driver);
			return true;
		});

		Scene scene = new Scene(root, 600, 400);
		stage.setTitle(this.getClass().getSimpleName());
		stage.setScene(scene);
		stage.show();
	}

	@Override
	public void stop() throws Exception {
		Platform.exit();
	}

	static void bindOtherControls(JmeForImageView jme, Controller controller) {
		controller.bgColor.valueProperty().addListener((ov, o, n) -> {
			jme.enqueue((jmeApp) -> {
				jmeApp.getViewPort().setBackgroundColor(new ColorRGBA((float)n.getRed(), (float)n.getGreen(), (float)n.getBlue(), (float)n.getOpacity()));
				return null;
			});
		});
		controller.bgColor.setValue(Color.LIGHTGRAY);

		controller.showStats.selectedProperty().addListener((ov, o, n) -> {
			jme.enqueue((jmeApp) -> {
				jmeApp.setDisplayStatView(n);
				jmeApp.setDisplayFps(n);
				return null;
			});
		});
		controller.showStats.setSelected(!controller.showStats.isSelected());

		controller.fpsReq.valueProperty().addListener((ov, o, n) -> {
			jme.enqueue((jmeApp) -> {
				AppSettings settings = new AppSettings(false);
				settings.setFullscreen(false);
				settings.setUseInput(false);
				settings.setFrameRate(n.intValue());
				settings.setCustomRenderer(com.jme3x.jfx.injfx.JmeContextOffscreenSurface.class);
				jmeApp.setSettings(settings);
				jmeApp.restart();
				return null;
			});
		});
		controller.fpsReq.setValue(30);
	}

	/**
	 * Create a similar scene to Tutorial "Hello Material" but without texture
	 * http://hub.jmonkeyengine.org/wiki/doku.php/jme3:beginner:hello_material
	 *
	 * @param jmeApp the application where to create a Scene
	 */
	static boolean createScene(SimpleApplication jmeApp) {
		Node rootNode = jmeApp.getRootNode();
		AssetManager assetManager = jmeApp.getAssetManager();

		/** A simple textured cube -- in good MIP map quality. */
		Box cube1Mesh = new Box( 1f,1f,1f);
		Geometry cube1Geo = new Geometry("My Textured Box", cube1Mesh);
		cube1Geo.setLocalTranslation(new Vector3f(-3f,1.1f,0f));
		Material cube1Mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		cube1Mat.setColor("Color", ColorRGBA.Blue);
		cube1Geo.setMaterial(cube1Mat);
		rootNode.attachChild(cube1Geo);

		/** A translucent/transparent texture, similar to a window frame. */
		Box cube2Mesh = new Box( 1f,1f,0.01f);
		Geometry cube2Geo = new Geometry("window frame", cube2Mesh);
		Material cube2Mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		cube2Mat.setColor("Color", ColorRGBA.Brown);
		cube2Geo.setQueueBucket(Bucket.Transparent);
		cube2Geo.setMaterial(cube2Mat);
		rootNode.attachChild(cube2Geo);

		/** A bumpy rock with a shiny light effect.*/
		Sphere sphereMesh = new Sphere(32,32, 2f);
		Geometry sphereGeo = new Geometry("Shiny rock", sphereMesh);
		sphereMesh.setTextureMode(Sphere.TextureMode.Projected); // better quality on spheres
		TangentBinormalGenerator.generate(sphereMesh);           // for lighting effect
		Material sphereMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		sphereMat.setBoolean("UseMaterialColors",true);
		sphereMat.setColor("Diffuse",ColorRGBA.Pink);
		sphereMat.setColor("Specular",ColorRGBA.White);
		sphereMat.setFloat("Shininess", 64f);  // [0,128]
		sphereGeo.setMaterial(sphereMat);
		sphereGeo.setLocalTranslation(0,2,-2); // Move it a bit
		sphereGeo.rotate(1.6f, 0, 0);          // Rotate it a bit
		rootNode.attachChild(sphereGeo);

		/** Must add a light to make the lit object visible! */
		DirectionalLight sun = new DirectionalLight();
		sun.setDirection(new Vector3f(1,0,-2).normalizeLocal());
		sun.setColor(ColorRGBA.White);
		rootNode.addLight(sun);
		
		rootNode.attachChild(makeFloor(jmeApp));
		return true;
	}

	/** A floor to show that the "shot" can go through several objects. */
	static protected Geometry makeFloor(SimpleApplication jmeApp) {
		Box box = new Box(15, .2f, 15);
		Geometry floor = new Geometry("the Floor", box);
		floor.setLocalTranslation(0, -4, -5);
		Material mat1 = new Material(jmeApp.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat1.setColor("Color", ColorRGBA.Gray);
		floor.setMaterial(mat1);
		return floor;
	}

	public static class Controller {

		@FXML
		public ImageView image;

		@FXML
		public ColorPicker bgColor;

		@FXML
		public CheckBox showStats;

		@FXML
		private Label fpsLabel;

		@FXML
		public Slider fpsReq;

		@FXML
		public void initialize() {
			//To resize image when parent is resize
			//image is wrapped into a "VBOX" or "HBOX" to allow resize smaller
			//see http://stackoverflow.com/questions/15951284/javafx-image-resizing
			Pane p = (Pane)image.getParent();
			image.fitHeightProperty().bind(p.heightProperty());
			image.fitWidthProperty().bind(p.widthProperty());

			fpsReq.valueProperty().addListener((ov, o, n) -> fpsLabel.setText(String.format("fps : %4d", n.intValue())));
			image.setPreserveRatio(false);
		}

	}
}
