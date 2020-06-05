package application;

import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import javafx.scene.control.Button;

public class MainController {
	Connection connection = null;
	PreparedStatement query = null;
	ResultSet result = null;
	String sqlquery;
	
	
	List<File> list;
	List<StackImage> ImageList;
	ITesseract tes = new Tesseract();
	CascadeClassifier plakaBulucu = new CascadeClassifier("eu.xml");
	private ObservableList <String> plateList;
	

	// a timer for acquiring the db datas
	private ScheduledExecutorService dbTimer;
	
	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that realizes the video capture
	private VideoCapture capture = new VideoCapture();
	// a flag to change the button behavior
	private boolean cameraActive = false;
	// the id of the camera to be used
	private static int cameraId = 0;
	
	@FXML
	private AnchorPane mainPanel;
	
	@FXML
	private ImageView mainImage;
	// the FXML area for showing the mask
	@FXML
	private ImageView blurredImage;
	@FXML
	private ImageView grayScaleImage;
	// the FXML area for showing the output of the morphological operations
	@FXML
	private ImageView thresHoldImage;
	// FXML slider for setting HSV ranges
	@FXML
	private ImageView dalitionErosionImage;
	@FXML
	private ImageView cannyImage;
	@FXML
	private ImageView cascadeImage; 
	@FXML
	private Slider hueStart;
	@FXML
	private Slider hueStop;
	@FXML
	private Slider saturationStart;
	@FXML
	private Slider saturationStop;
	@FXML
	private Slider valueStart;
	@FXML
	private Slider valueStop;
	// FXML label to show the current values set with the sliders
	
	@FXML
	private MenuItem loginBTN;
	
	@FXML
	private MenuItem openFile;
	
	@FXML
	private Button startCam;
	
	
	public void initialize() 
	{
		connection = dbClass.Connect();
		if(connection != null) 
		{
			this.mainPanel.setDisable(false);
		}
	}
	
	public MainController() 
	{
		ImageList = new ArrayList<StackImage>();
		plateList = FXCollections.observableArrayList();
	}

	public void selectFile() 
	{
		Stage stage = null;
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");
		fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("Image Files", "*.png; *.jpg; *.jpeg")
				);
		list = fileChooser.showOpenMultipleDialog(stage);
		if(list != null) 
		{
			for(File f : list) 
			{
				ImageList.add(new StackImage(f));
			}
			mainImage.setImage(ImageList.get(0).img);
			grayScaleImage.setImage(ImageList.get(0).setRGBToGrayScale());
		}
	}
	
	@FXML
	private void startCamera(ActionEvent event) 
	{
		
		// set a fixed width for all the image to show and preserve image ratio
		this.imageViewProperties(this.mainImage, 400);
		this.imageViewProperties(this.blurredImage, 400);
		this.imageViewProperties(this.grayScaleImage, 400);
		this.imageViewProperties(this.thresHoldImage, 400);
		this.imageViewProperties(this.dalitionErosionImage, 400);
		this.imageViewProperties(this.cannyImage, 400);
		if (!this.cameraActive)
		{
			// start the video capture
			this.capture.open(cameraId);
			
			// is the video stream available?
			if (this.capture.isOpened())
			{
				this.cameraActive = true;
				
				// grab a frame every 33 ms (30 frames/sec)
				Runnable frameGrabber = new Runnable() {
					
					@Override
					public void run()
					{
						// effectively grab and process a single frame
						Mat frame = grabFrame();
						// convert and show the frame
						Image imageToShow = StackImage.mat2Image(frame);
						updateImageView(mainImage, imageToShow);
						
					}
				};
				
				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
				
				Runnable sqlQueryRunner = new Runnable() 
				{
					@Override
					public void run() 
					{
						if(plateList.size() != 0)
						{
					
					        try{
					        	
					            int cc =  Integer.parseInt(plateList.get(0).substring(0, 2));
					            sqlquery = "SElECT * FROM araclar WHERE plaka = ?";
				        		try {
									query = connection.prepareStatement(sqlquery);
									query.setString(1, plateList.get(0));
									result = query.executeQuery();
									//System.out.println(plateList.get(0));
									if(!result.next()) 
									{
										//System.err.println(plateList.get(0) + " Not Found in System");
										//Alert alert = new Alert(AlertType.NONE, "Not Found Car", ButtonType.OK);
										//alert.showAndWait();
									}else 
									{
										System.err.println("Ad: " + result.getString("ad") + " Soyad: " + result.getString("soyad") + " Plaka: " + result.getString("plaka") + " Araç Tipi: " + result.getString("aracTip"));
									}
									plateList.remove(0);
									
								} catch (SQLException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
					        }catch(Exception e){
					        	plateList.remove(0);
					            //e.printStackTrace();
					        }
							
							
						}
						
					}
				};
				this.dbTimer = Executors.newSingleThreadScheduledExecutor();
				this.dbTimer.scheduleAtFixedRate(sqlQueryRunner, 0, 33, TimeUnit.MILLISECONDS);
				
				// update the button content
				this.startCam.setText("Stop Camera");
			}
			else
			{
				// log the error
				System.err.println("Impossible to open the camera connection...");
			}
		}
		else
		{
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the button content
			this.startCam.setText("Start Camera");
			
			// stop the timer
			this.stopAcquisition();
		}
	}
	
	/**
	 * Get a frame from the opened video stream (if any)
	 *
	 * @return the {@link Mat} to show
	 */
	private Mat grabFrame()
	{
		// init everything
		Mat frame = new Mat();
		
		// check if the capture is open
		if (this.capture.isOpened())
		{
			try
			{
				// read the current frame
				this.capture.read(frame);
				
				// if the frame is not empty, process it
				if (!frame.empty())
				{
					// init
					Mat blurredImage = new Mat();
					Mat grayscaleImage = new Mat();
					Mat thresholdImage = new Mat();
					Mat diolaterosImage = new Mat();
					Mat cannyImage = new Mat();
					
					// remove some noise
					Imgproc.GaussianBlur(frame, blurredImage, new Size(5, 5), 1);
					this.updateImageView(this.blurredImage, StackImage.mat2Image(blurredImage));
					
					Imgproc.cvtColor(blurredImage, grayscaleImage, Imgproc.COLOR_BGR2GRAY);
					this.updateImageView(this.grayScaleImage, StackImage.mat2Image(grayscaleImage));
					
					//Imgproc.threshold(grayscaleImage, thresholdImage, 200, 255, Imgproc.THRESH_BINARY);
					Imgproc.adaptiveThreshold(grayscaleImage, thresholdImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);
					this.updateImageView(this.thresHoldImage, StackImage.mat2Image(thresholdImage));
					
					
					 //cvCanny(gray_image, gray_image, 5.1, 1);
					  MatOfRect detectGiris = new MatOfRect();
					  
					  //plakaBulucu.detectMultiScale(resimGiris, detectGiris,1.1, 1,1, new Size(20, 20), new Size(300, 300));
					  plakaBulucu.detectMultiScale(frame, detectGiris);
					  Rect[] plates = detectGiris.toArray();
					  for(int i = 0; i < 1 && plates.length > 0; i++) 
					  {
						  Imgproc.rectangle(blurredImage, new Point(plates[i].x, plates[i].y), new Point(plates[i].x + plates[i].width, plates[i].y + plates[i].height), new Scalar(0, 255,0));
						  this.updateImageView(this.blurredImage, StackImage.mat2Image(blurredImage));
						  PixelReader reader = StackImage.mat2Image(thresholdImage).getPixelReader();
						  WritableImage newImage = new WritableImage(reader, plates[i].x, plates[i].y, plates[i].width, plates[i].height);
						  this.updateImageView(this.cascadeImage, newImage);
						  try 
						  {
							  BufferedImage blurredBuf = SwingFXUtils.fromFXImage(this.cascadeImage.getImage(), null);
							  tes.setLanguage("tur");
							  String res = tes.doOCR(blurredBuf);
							  this.plateList.add(res.replaceAll("\\s", ""));
						  }catch(TesseractException ex) 
						  {
							  ex.printStackTrace();
						  }
					  }

					  
					  
					// convert the frame to HSV
					
					Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24, 24));
					Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12, 12));
					
					Imgproc.erode(thresholdImage, diolaterosImage, erodeElement);
					
					
					Imgproc.dilate(diolaterosImage, diolaterosImage, dilateElement);
					
					
					this.updateImageView(this.dalitionErosionImage, StackImage.mat2Image(diolaterosImage));
					
					Mat resultImage = new Mat();
					//frame = this.findAndDrawBalls(morphOutput, frame);
					Imgproc.Canny(diolaterosImage, cannyImage, 50, 150, 3, false);
					frame.copyTo(resultImage, cannyImage);
					this.updateImageView(this.cannyImage, StackImage.mat2Image(resultImage));
					frame = this.findAndDrawPlates(diolaterosImage, frame);
					
					
				
					
				}
				
			}
			catch (Exception e)
			{
				// log the (full) error
				System.err.print("Exception during the image elaboration...");
				e.printStackTrace();
			}
		}
		
		return frame;
	}
	
	/**
	 * Given a binary image containing one or more closed surfaces, use it as a
	 * mask to find and highlight the objects contours
	 * 
	 * @param maskedImage
	 *            the binary image to be used as a mask
	 * @param frame
	 *            the original {@link Mat} image to be used for drawing the
	 *            objects contours
	 * @return the {@link Mat} image with the objects contours framed
	 */
	private Mat findAndDrawPlates(Mat maskedImage, Mat frame)
	{
		// init
		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();
		
		// find contours
		Imgproc.findContours(maskedImage, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
		
		// if any contour exist...
		if (hierarchy.size().height > 0 && hierarchy.size().width > 0)
		{
			// for each contour, display it in blue
			for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0])
			{
				Imgproc.drawContours(frame, contours, idx, new Scalar(250, 0, 0));
			}
		}
		
		return frame;
	}
	
	/**
	 * Set typical {@link ImageView} properties: a fixed width and the
	 * information to preserve the original image ration
	 * 
	 * @param image
	 *            the {@link ImageView} to use
	 * @param dimension
	 *            the width of the image to set
	 */
	private void imageViewProperties(ImageView image, int dimension)
	{
		// set a fixed width for the given ImageView
		image.setFitWidth(dimension);
		// preserve the image ratio
		image.setPreserveRatio(true);
	}
	
	/**
	 * Stop the acquisition from the camera and release all the resources
	 */
	private void stopAcquisition()
	{
		if (this.timer!=null && !this.timer.isShutdown())
		{
			try
			{
				// stop the timer
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				// log any exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
		}
		if (this.dbTimer != null && !this.dbTimer.isShutdown())
		{
			try
			{
				// stop the timer
				this.dbTimer.shutdown();
				this.dbTimer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				// log any exception
				System.err.println("Exception in stopping the db Requests! " + e);
			}
		}
		
		if (this.capture.isOpened())
		{
			// release the camera
			this.capture.release();
		}
	}
	
	/**
	 * Update the {@link ImageView} in the JavaFX main thread
	 * 
	 * @param view
	 *            the {@link ImageView} to update
	 * @param image
	 *            the {@link Image} to show
	 */
	private void updateImageView(ImageView view, Image image)
	{
		StackImage.onFXThread(view.imageProperty(), image);
	}
	
	/**
	 * On application close, stop the acquisition from the camera
	 */
	protected void setClosed()
	{
		this.stopAcquisition();
	}
}
