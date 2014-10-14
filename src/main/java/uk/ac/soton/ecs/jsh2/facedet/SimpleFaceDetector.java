package uk.ac.soton.ecs.jsh2.facedet;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayAdapter;
import org.openimaj.video.capture.VideoCapture;

/**
 * Really simple face detection tool. Can be used in gui mode, in which case it
 * opens a webcam, and performs face detection in real-time (allowing the user
 * to save images). Additionally has a command-line mode for processing of
 * previously captured images.
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class SimpleFaceDetector {
	private HaarCascadeDetector detector;

	public SimpleFaceDetector() {
		detector = HaarCascadeDetector.BuiltInCascade.frontalface_alt2.load();
		detector.setMinSize(80);
	}

	public void findAndDrawFaces(MBFImage image) {
		for (final DetectedFace face : detector.detectFaces(image.flatten())) {
			image.drawShape(face.getBounds(), 3, RGBColour.RED);
		}
	}

	private void liveMode() {
		// start the webcam gui
		if (VideoCapture.getVideoDevices().size() == 0) {
			JOptionPane.showMessageDialog(null, "No video capture devices found.");
			System.exit(1);
		} else {
			try {
				final VideoCapture vc = new VideoCapture(640, 480);
				final VideoDisplay<MBFImage> display = VideoDisplay.createVideoDisplay(vc);
				final MBFImage lastFrame = new MBFImage(640, 480);
				display.addVideoListener(new VideoDisplayAdapter<MBFImage>() {
					@Override
					public void beforeUpdate(MBFImage frame) {
						if (!display.isPaused()) {
							findAndDrawFaces(frame);
							lastFrame.drawImage(frame, 0, 0);
						}
					}
				});
				final JFrame frame = (JFrame) SwingUtilities.getRoot(display.getScreen());
				frame.setTitle("Live video. Press space to pause and s to save.");
				frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

				frame.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent e) {
						if (e.getKeyCode() == KeyEvent.VK_SPACE) {
							display.togglePause();
						} else if (e.getKeyCode() == KeyEvent.VK_S) {
							synchronized (display) {
								final JFileChooser fc = new JFileChooser() {
									private static final long serialVersionUID = 1L;

									@Override
									public void approveSelection() {
										final File f = getSelectedFile();
										if (f.exists() && getDialogType() == SAVE_DIALOG) {
											final Object[] options = { "Cancel", "Replace" };
											final int result = JOptionPane
													.showOptionDialog(
															this,
															"A file or folder with the same name already exists in the folder "
																	+ f.getParentFile().getName()
																	+ ". Replacing it will overwrite its current contents.",
															"“" + f.getName()
																	+ "” already exists. Do you want to replace it?",
															JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE,
															null, options, options[0]);
											switch (result) {
											case 1:
												super.approveSelection();
												return;
											case 0:
												return;
											}
										}
										super.approveSelection();
									}
								};

								final FileNameExtensionFilter defaultFilter = new FileNameExtensionFilter("JPEG (.jpg)",
										"jpg", "jpeg");

								fc.addChoosableFileFilter(defaultFilter);
								fc.addChoosableFileFilter(new FileNameExtensionFilter("PNG (.png)", "png"));

								fc.setFileFilter(defaultFilter);
								fc.setSelectedFile(new File("image." + defaultFilter.getDefaultExtension()));

								if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
									final File file = fc.getSelectedFile();

									try {
										ImageUtilities.write(lastFrame,
												((FileNameExtensionFilter) fc.getFileFilter()).getDefaultExtension(),
												file);
									} catch (final IOException e1) {
										JOptionPane.showMessageDialog(frame, e1);
									}
								}
							}
						}
					}
				});
			} catch (final IOException e) {
				JOptionPane.showMessageDialog(null, "Error using video capture device.");
			}
		}
	}

	private void cmdLineMode(String in, String out) {
		try {
			final File inf = new File(in);
			final File outf = new File(out);

			final InputStream is = null;
			try {
				final MBFImage image = ImageUtilities.readMBF(inf);
				findAndDrawFaces(image);

				if (!outf.getName().contains(".")) {
					ImageUtilities.write(image, "jpeg", outf);
				} else {
					ImageUtilities.write(image, outf);
				}
			} finally {
				if (is != null)
					is.close();
			}
		} catch (final IOException e) {
			System.err.println("Error: " + e);
		}
	}

	public static void main(String[] args) {
		final SimpleFaceDetector sfd = new SimpleFaceDetector();

		if (args.length == 0) {
			sfd.liveMode();
		} else if (args.length == 2) {
			sfd.cmdLineMode(args[0], args[1]);
		} else {
			final String name = new File(SimpleFaceDetector.class.getProtectionDomain()
					.getCodeSource().getLocation().getPath()).getName();

			System.err.println("Webcam mode usage:");
			System.err.println("java -jar " + name);
			System.err.println();
			System.err.println("Commandline mode usage:");
			System.err.println("java -jar " + name + " <input_image> <output_image>");
			System.err.println();
		}
	}
}
