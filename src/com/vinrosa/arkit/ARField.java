package com.vinrosa.arkit;

import net.rim.device.api.math.Fixed32;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.EncodedImage;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;

public class ARField extends Field {
	private static final int DEFAULT_WIDTH = 150;
	private static final int DEFAULT_HEIGHT = 100;
	private EncodedImage icon;
	private String name;

	public ARSize size;

	private int minFontSize;

	public ARField(String name, EncodedImage icon) {
		this.icon = icon;
		this.name = name;
		this.size = new ARSize();
		this.size.width = DEFAULT_WIDTH;
		this.size.height = DEFAULT_HEIGHT;
		this.minFontSize = 5;
	}

	protected void layout(int _width, int _height) {
		super.setExtent(size.width, size.height);

		if (this.icon.getHeight() > size.height - getFont().getHeight()) {
			this.icon = this.resizeImage(this.icon, size.height - getFont().getHeight(), size.height - getFont().getHeight());
		}
		Font font = getFont();
		while (font.getAdvance(this.name) > this.size.width) {
			if (font.getHeight() <= minFontSize)
				break;
			font = font.derive(font.getStyle(), font.getHeight() - 1);
		}
		setFont(font);
	}

	protected void paint(Graphics graphics) {
		if (icon != null) {
			Bitmap b = this.icon.getBitmap();
			graphics.drawBitmap((this.size.width >> 1) - (b.getWidth() >> 1), 0, b.getWidth(), b.getHeight(), b, 0, 0);
		}
		graphics.drawText(name, (this.size.width >> 1) - (getFont().getAdvance(this.name) >> 1), getHeight() - getFont().getHeight());
	}

	public EncodedImage resizeImage(EncodedImage image, int width, int height) {
		if (image == null) {
			return null;
		}
		if (width == 0 || height == 0) {
			return image;
		}

		// return if image does not need a resizing
		if (image.getWidth() <= width && image.getHeight() <= height) {
			return image;
		}

		double scaleHeight, scaleWidth;
		if (image.getWidth() > width && image.getHeight() > height) {
			if (image.getWidth() > image.getHeight()) {
				scaleWidth = width;
				scaleHeight = (double) width / image.getWidth() * image.getHeight();
			} else { // scale with height
				scaleHeight = height;
				scaleWidth = (double) height / image.getHeight() * image.getWidth();
			}
		} else if (width < image.getWidth()) { // scale with scale width or
												// height
			scaleWidth = width;
			scaleHeight = (double) width / image.getWidth() * image.getHeight();
		} else {
			scaleHeight = height;
			scaleWidth = (double) height / image.getHeight() * image.getWidth();
		}
		int w = Fixed32.div(Fixed32.toFP(image.getWidth()), Fixed32.toFP((int) scaleWidth));
		int h = Fixed32.div(Fixed32.toFP(image.getHeight()), Fixed32.toFP((int) scaleHeight));
		return image.scaleImage32(w, h);
	}

	public ARSize getBoxSize() {
		return size;
	}

	public void setScaleFactor(double scaleFactor) {
		size.width = (int) (DEFAULT_WIDTH * scaleFactor);
		size.height = (int) (DEFAULT_HEIGHT * scaleFactor);
	}
}
