package com.prairie.eevernote.ui;

import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.prairie.eevernote.Constants;
import com.prairie.eevernote.EEProperties;
import com.prairie.eevernote.ui.GeomRectangle.Position;
import com.prairie.eevernote.util.ColorUtil;
import com.prairie.eevernote.util.ImageUtil;
import com.prairie.eevernote.util.RunningCounter;

@SuppressWarnings("serial")
public class CaptureView extends JFrame implements Constants {

	private BufferedImage fullScreen;
	private GeomRectangle rectangle = new GeomRectangle();
	private boolean isCapturing = false;
	private boolean isCaptured = false;
	private boolean isCaptureFullScreenViaClick = false;
	private RunningCounter counter = new RunningCounter();
	private GeomPoint datumPoint;

	public CaptureView() throws HeadlessException, AWTException {

		this.fullScreen = ImageUtil.captureScreen(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
		setSize(Toolkit.getDefaultToolkit().getScreenSize());
		setUndecorated(true);
		resetView();

		addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					rectangle.clear();
					setVisible(false);
					dispose();
				}
			}
		});
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					if (e.getClickCount() == ONE) {
						if (!isCaptured) {
							isCaptureFullScreenViaClick = true;
							rectangle.getStartPoint().setLocation(ZERO, ZERO);
							rectangle.getEndPoint().setLocation(new Double(Toolkit.getDefaultToolkit().getScreenSize().getWidth()).intValue(), new Double(Toolkit.getDefaultToolkit().getScreenSize().getHeight()).intValue());
							maskFullScreen(EEPLUGIN_SCREENSHOT_MASK_FULLSCREEN_SCALEFACTOR);
							repaint();
							isCaptured = true;
						}
					} else if (e.getClickCount() == TWO) {
						setVisible(false);
						dispose();
					}
				} else if (e.getButton() == MouseEvent.BUTTON3) {
					if (e.getClickCount() == ONE) {
						if (isCaptured) {
							resetView();
							isCaptured = false;
							isCaptureFullScreenViaClick = false;
							setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
						} else {
							rectangle.clear();
							setVisible(false);
							dispose();
						}
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (!isCaptured && (e.getButton() == MouseEvent.BUTTON1)) {
					rectangle.getStartPoint().setLocation(e.getX(), e.getY());
					isCapturing = true;
					counter.resetTimes(ONE);
				} else if (isResize()) {
					datumPoint = new GeomPoint(e.getX(), e.getY());
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (isCapturing && (e.getButton() == MouseEvent.BUTTON1)) {
					rectangle.getEndPoint().setLocation(e.getX(), e.getY());
					isCapturing = false;
					isCaptured = rectangle.getWidth() > ZERO && rectangle.getHeight() > ZERO;
				}
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (isCapturing) {
					if (counter.hasTimes()) {
						maskFullScreen(EEPLUGIN_SCREENSHOT_MASK_FULLSCREEN_SCALEFACTOR);
					}
					rectangle.getEndPoint().setLocation(e.getX(), e.getY());
					if (rectangle.isRealRectangle()) {
						repaint();
					}
				} else if (isResize()) {
					if (getCursor() == Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)) {
						rectangle.resize(Position.EAST, e.getX() - datumPoint.getX(), ZERO);
					} else if (getCursor() == Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR)) {
						rectangle.resize(Position.SOUTH, ZERO, e.getY() - datumPoint.getY());
					} else if (getCursor() == Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)) {
						rectangle.resize(Position.WEST, e.getX() - datumPoint.getX(), ZERO);
					} else if (getCursor() == Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)) {
						rectangle.resize(Position.NORTH, ZERO, e.getY() - datumPoint.getY());
					} else if (getCursor() == Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR)) {
						rectangle.resize(Position.NORTHEAST, e.getX() - datumPoint.getX(), e.getY() - datumPoint.getY());
					} else if (getCursor() == Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR)) {
						rectangle.resize(Position.NORTHWEST, e.getX() - datumPoint.getX(), e.getY() - datumPoint.getY());
					} else if (getCursor() == Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR)) {
						rectangle.resize(Position.SOUTHEAST, e.getX() - datumPoint.getX(), e.getY() - datumPoint.getY());
					} else if (getCursor() == Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR)) {
						rectangle.resize(Position.SOUTHWEST, e.getX() - datumPoint.getX(), e.getY() - datumPoint.getY());
					} else if (getCursor() == Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)) {
						rectangle.move(e.getX() - datumPoint.getX(), e.getY() - datumPoint.getY());
					}
					datumPoint.move(e.getX() - datumPoint.getX(), e.getY() - datumPoint.getY());
					repaint();
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				if (!isCapturing && !isCaptured) {
					rectangle.getStartPoint().setLocation(e.getX(), e.getY());
				} else if (isCaptured) {
					if (rectangle.positionOf(new GeomPoint(e.getX(), e.getY())) == GeomRectangle.Position.EAST) {
						setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
					} else if (rectangle.positionOf(new GeomPoint(e.getX(), e.getY())) == GeomRectangle.Position.SOUTH) {
						setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
					} else if (rectangle.positionOf(new GeomPoint(e.getX(), e.getY())) == GeomRectangle.Position.WEST) {
						setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
					} else if (rectangle.positionOf(new GeomPoint(e.getX(), e.getY())) == GeomRectangle.Position.NORTH) {
						setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
					} else if (rectangle.positionOf(new GeomPoint(e.getX(), e.getY())) == GeomRectangle.Position.NORTHEAST) {
						setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
					} else if (rectangle.positionOf(new GeomPoint(e.getX(), e.getY())) == GeomRectangle.Position.NORTHWEST) {
						setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
					} else if (rectangle.positionOf(new GeomPoint(e.getX(), e.getY())) == GeomRectangle.Position.SOUTHEAST) {
						setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
					} else if (rectangle.positionOf(new GeomPoint(e.getX(), e.getY())) == GeomRectangle.Position.SOUTHWEST) {
						setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
					} else if (rectangle.positionOf(new GeomPoint(e.getX(), e.getY())) == GeomRectangle.Position.INSIDE) {
						setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
					} else {
						setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					}
				}
			}
		});
	}

	@Override
	public void paint(Graphics graphics) {
		super.paint(graphics);
		if (isCapturing || isCaptureFullScreenViaClick || isResize()) {
			Image cropedScreenshot = fullScreen.getSubimage(rectangle.getTopLeftPoint().getX(), rectangle.getTopLeftPoint().getY(), rectangle.getWidth(), rectangle.getHeight());
			Graphics2D g2 = (Graphics2D) graphics;
			g2.drawImage(cropedScreenshot, rectangle.getTopLeftPoint().getX(), rectangle.getTopLeftPoint().getY(), null);

			g2.setColor(ColorUtil.AWT_EVERNOTE_GREEN);
			g2.drawRect(rectangle.getTopLeftPoint().getX(), rectangle.getTopLeftPoint().getY(), rectangle.getWidth(), rectangle.getHeight());

			g2.drawRect(rectangle.getTopLeftRectangle().getTopLeftPoint().getX(), rectangle.getTopLeftRectangle().getTopLeftPoint().getY(), rectangle.getTopLeftRectangle().getWidth(), rectangle.getTopLeftRectangle().getHeight());
			g2.fillRect(rectangle.getTopLeftRectangle().getTopLeftPoint().getX(), rectangle.getTopLeftRectangle().getTopLeftPoint().getY(), rectangle.getTopLeftRectangle().getWidth(), rectangle.getTopLeftRectangle().getHeight());

			g2.drawRect(rectangle.getTopRightRectangle().getTopLeftPoint().getX(), rectangle.getTopRightRectangle().getTopLeftPoint().getY(), rectangle.getTopRightRectangle().getWidth(), rectangle.getTopRightRectangle().getHeight());
			g2.fillRect(rectangle.getTopRightRectangle().getTopLeftPoint().getX(), rectangle.getTopRightRectangle().getTopLeftPoint().getY(), rectangle.getTopRightRectangle().getWidth(), rectangle.getTopRightRectangle().getHeight());

			g2.drawRect(rectangle.getBottomLeftRectangle().getTopLeftPoint().getX(), rectangle.getBottomLeftRectangle().getTopLeftPoint().getY(), rectangle.getBottomLeftRectangle().getWidth(), rectangle.getBottomLeftRectangle().getHeight());
			g2.fillRect(rectangle.getBottomLeftRectangle().getTopLeftPoint().getX(), rectangle.getBottomLeftRectangle().getTopLeftPoint().getY(), rectangle.getBottomLeftRectangle().getWidth(), rectangle.getBottomLeftRectangle().getHeight());

			g2.drawRect(rectangle.getBottomRightRectangle().getTopLeftPoint().getX(), rectangle.getBottomRightRectangle().getTopLeftPoint().getY(), rectangle.getBottomRightRectangle().getWidth(), rectangle.getBottomRightRectangle().getHeight());
			g2.fillRect(rectangle.getBottomRightRectangle().getTopLeftPoint().getX(), rectangle.getBottomRightRectangle().getTopLeftPoint().getY(), rectangle.getBottomRightRectangle().getWidth(), rectangle.getBottomRightRectangle().getHeight());

			g2.drawRect(rectangle.getTopRectangle().getTopLeftPoint().getX(), rectangle.getTopRectangle().getTopLeftPoint().getY(), rectangle.getTopRectangle().getWidth(), rectangle.getTopRectangle().getHeight());
			g2.fillRect(rectangle.getTopRectangle().getTopLeftPoint().getX(), rectangle.getTopRectangle().getTopLeftPoint().getY(), rectangle.getTopRectangle().getWidth(), rectangle.getTopLeftRectangle().getHeight());

			g2.drawRect(rectangle.getBottomRectangle().getTopLeftPoint().getX(), rectangle.getBottomRectangle().getTopLeftPoint().getY(), rectangle.getBottomRectangle().getWidth(), rectangle.getBottomRectangle().getHeight());
			g2.fillRect(rectangle.getBottomRectangle().getTopLeftPoint().getX(), rectangle.getBottomRectangle().getTopLeftPoint().getY(), rectangle.getBottomRectangle().getWidth(), rectangle.getBottomRectangle().getHeight());

			g2.drawRect(rectangle.getLeftRectangle().getTopLeftPoint().getX(), rectangle.getLeftRectangle().getTopLeftPoint().getY(), rectangle.getLeftRectangle().getWidth(), rectangle.getLeftRectangle().getHeight());
			g2.fillRect(rectangle.getLeftRectangle().getTopLeftPoint().getX(), rectangle.getLeftRectangle().getTopLeftPoint().getY(), rectangle.getLeftRectangle().getWidth(), rectangle.getLeftRectangle().getHeight());

			g2.drawRect(rectangle.getRightRectangle().getTopLeftPoint().getX(), rectangle.getRightRectangle().getTopLeftPoint().getY(), rectangle.getRightRectangle().getWidth(), rectangle.getRightRectangle().getHeight());
			g2.fillRect(rectangle.getRightRectangle().getTopLeftPoint().getX(), rectangle.getRightRectangle().getTopLeftPoint().getY(), rectangle.getRightRectangle().getWidth(), rectangle.getRightRectangle().getHeight());

			GeomPoint p = rectangle.getTopLeftPoint();
			if (p.getY() - (EEPLUGIN_SCREENSHOT_HINT_HEIGHT + TWO) < ZERO) {
				p = new GeomPoint(rectangle.getTopLeftPoint().getX(), rectangle.getTopLeftPoint().getY() + (EEPLUGIN_SCREENSHOT_HINT_HEIGHT + TWO));
			}
			g2.drawImage(ImageUtil.mask(fullScreen.getSubimage(p.getX(), p.getY() - (EEPLUGIN_SCREENSHOT_HINT_HEIGHT + TWO), EEPLUGIN_SCREENSHOT_HINT_WIDTH, EEPLUGIN_SCREENSHOT_HINT_HEIGHT), EEPLUGIN_SCREENSHOT_HINT_SCALEFACTOR), p.getX(), p.getY() - (EEPLUGIN_SCREENSHOT_HINT_HEIGHT + TWO), null);
			g2.setColor(Color.WHITE);
			g2.setFont(getFont().deriveFont(Font.BOLD));
			g2.drawString(EEProperties.getProperties().getProperty(EECLIPPERPLUGIN_ACTIONDELEGATE_CLIPSCREENSHOTTOEVERNOTE_HINT), p.getX() + EEPLUGIN_SCREENSHOT_HINT_TEXT_START_X, p.getY() + EEPLUGIN_SCREENSHOT_HINT_TEXT_START_Y);
		}
	}

	private boolean isResize() {
		return (getCursor() == Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)) || (getCursor() == Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR)) || (getCursor() == Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)) || (getCursor() == Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)) || (getCursor() == Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR)) || (getCursor() == Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR)) || (getCursor() == Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR)) || (getCursor() == Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR)) || (getCursor() == Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
	}

	private void maskFullScreen(final float scaleFactor) {
		setContentPane(new JPanel() {
			@Override
			public void paintComponent(Graphics graphics) {
				super.paintComponent(graphics);
				((Graphics2D) graphics).drawImage(ImageUtil.mask(fullScreen, scaleFactor), ZERO, ZERO, null);
			}
		});
		requestFocus();
		setAlwaysOnTop(true);
		setVisible(true);
	}

	private void resetView() {
		setContentPane(new JPanel() {
			@Override
			public void paintComponent(Graphics graphics) {
				super.paintComponent(graphics);
				Graphics2D g2 = (Graphics2D) graphics;
				g2.drawImage(fullScreen, ZERO, ZERO, null);
				g2.setColor(ColorUtil.AWT_EVERNOTE_GREEN);
				g2.setStroke(new BasicStroke(SIX));
				g2.drawRect(ZERO, ZERO, new Double(Toolkit.getDefaultToolkit().getScreenSize().getWidth()).intValue(), new Double(Toolkit.getDefaultToolkit().getScreenSize().getHeight()).intValue());
			}
		});
		setAlwaysOnTop(true);
		setVisible(true);
		requestFocus();
	}

	public BufferedImage getScreenshot() {
		if (!rectangle.isRealRectangle()) {
			return null;
		}
		return this.fullScreen.getSubimage(rectangle.getTopLeftPoint().getX(), rectangle.getTopLeftPoint().getY(), rectangle.getWidth(), rectangle.getHeight());
	}

	public static BufferedImage showView() throws HeadlessException, AWTException, InterruptedException {
		final CaptureView view = new CaptureView();
		view.setVisible(true);
		while (view.isVisible()) {
			Thread.sleep(100);
		}
		return view.getScreenshot();
	}
}