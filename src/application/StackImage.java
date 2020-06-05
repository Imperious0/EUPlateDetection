package application;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class StackImage {

	//Loaded Image With Bytes
	public Image img;
	public BufferedImage chImg;
	private int[][] pixels;
	//Image Informations
	public String Name;
	public double Width;
	public double Height;
	public long Size;
	
	public StackImage(File f)
	{
		gatherInfo(f);
	}
	
	//Function for Gathering Image Infos
	private void gatherInfo(File f) 
	{
		this.img = new Image(f.toURI().toString());
		this.chImg = SwingFXUtils.fromFXImage(img, null);
		
		this.pixels = new int[chImg.getHeight()][chImg.getWidth()];
		this.getPixelInfo();
	
		this.Name = f.getName();
		this.Width = img.getWidth();
		this.Height = img.getHeight();
		this.Size = f.length();
	}
	private void getPixelInfo() 
	{
		for(int i = 0; i < this.pixels.length; i++)
			for(int j = 0; j < this.pixels[i].length; j++) 
				this.pixels[i][j] = this.chImg.getRGB(j, i);
	}
	public Image setRGBToGrayScale() 
	{
		int[][] tmpPixels = new int [this.pixels.length][this.pixels[0].length];
		BufferedImage tmpBuffered = SwingFXUtils.fromFXImage(this.img, null);
		for(int i = 0; i < this.pixels.length; i++) 
		{
			for(int j = 0; j < this.pixels[i].length; j++) 
			{
				Color tmpColor = new Color(this.pixels[i][j]);
				int red = (int)(tmpColor.getRed() * 0.2126);
				int green = (int)(tmpColor.getGreen() * 0.7152);
				int blue = (int)(tmpColor.getBlue() * 0.0722);
				int SumColor = red + green + blue;
				
				Color shadeOfGrayPixel = new Color (SumColor, SumColor, SumColor);
				tmpPixels[i][j] = shadeOfGrayPixel.getRGB();
				tmpBuffered.setRGB(j, i, tmpPixels[i][j]);
			}
		}
		Image tmpImage = SwingFXUtils.toFXImage(tmpBuffered, null);
		
		return tmpImage;
	}
	
	/**
	 * Generic method for putting element running on a non-JavaFX thread on the
	 * JavaFX thread, to properly update the UI
	 * 
	 * @param property
	 *            a {@link ObjectProperty}
	 * @param value
	 *            the value to set for the given {@link ObjectProperty}
	 */
	public static <T> void onFXThread(final ObjectProperty<T> property, final T value)
	{
		Platform.runLater(() -> {
			property.set(value);
		});
	}
	
	/**
	 * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
	 *
	 * @param frame
	 *            the {@link Mat} representing the current frame
	 * @return the {@link Image} to show
	 */
	public static Image mat2Image(Mat frame)
	{
		try
		{
			return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
		}
		catch (Exception e)
		{
			System.err.println("Cannot convert the Mat obejct: " + e);
			return null;
		}
	}
	
	/**
	 * Support for the {@link mat2image()} method
	 * 
	 * @param original
	 *            the {@link Mat} object in BGR or grayscale
	 * @return the corresponding {@link BufferedImage}
	 */
	private static BufferedImage matToBufferedImage(Mat original)
	{
		// init
		BufferedImage image = null;
		int width = original.width(), height = original.height(), channels = original.channels();
		byte[] sourcePixels = new byte[width * height * channels];
		original.get(0, 0, sourcePixels);
		
		if (original.channels() > 1)
		{
			image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		}
		else
		{
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		}
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
		ImageView imageView = new ImageView(SwingFXUtils.toFXImage(image, null));
		return image;
	}

}
