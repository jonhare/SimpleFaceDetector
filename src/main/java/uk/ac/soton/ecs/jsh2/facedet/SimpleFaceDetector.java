package uk.ac.soton.ecs.jsh2.facedet;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayAdapter;
import org.openimaj.video.capture.VideoCapture;

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
								final JFileChooser fc = new JFileChooser();
								fc.addChoosableFileFilter(new FileNameExtensionFilter("PNG (.png)", "png"));
								fc.addChoosableFileFilter(new FileNameExtensionFilter("JPEG (.jpg)", "jpg", "jpeg"));
								fc.setSelectedFile(new File("image."
										+ ((FileNameExtensionFilter) fc.getFileFilter()).getDefaultExtension()));

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
	
	private void cmdLineMode(String string, String string2) {
		for (final String arg : args) {
			try {
				final FileSystemManager fsManager = VFS.getManager();
				final FileObject path = fsManager.resolveFile(arg);

				final List<JFrame> frames = new ArrayList<JFrame>();

				InputStream is = null;
				try {
					is = path.getContent().getInputStream();
					final MBFImage image = ImageUtilities.readMBF(is);
					sfd.findAndDrawFaces(image);
					final JFrame frame = DisplayUtilities.displaySimple(image, path.getName().toString());
					frames.add(frame);
					frame.addWindowListener(new WindowAdapter() {
						@Override
						public void windowClosed(WindowEvent e) {
							synchronized (frames) {
								frames.remove(frame);

								if (frames.isEmpty()) {
									System.exit(0);
								}
							}
						}
					});
				} finally {
					if (is != null)
						is.close();
				}
			} catch (final IOException e) {
				System.err.println("Error reading " + arg);
			}
		}
	}
	}

	public static void main(String[] args) {
		final SimpleFaceDetector sfd = new SimpleFaceDetector();

		if (args.length == 0) {
			sfd.liveMode();
		} else if (args.length == 2) {
			sfd.cmdLineMode(args[0], args[1]);
		}
	}
}
