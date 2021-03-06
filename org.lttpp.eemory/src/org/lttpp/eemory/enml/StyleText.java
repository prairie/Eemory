package org.lttpp.eemory.enml;

import org.apache.commons.lang3.StringUtils;
import org.lttpp.eemory.util.ColorUtil;

public class StyleText {

    private String text;
    private String face;
    private String colorHexCode;
    private String size;
    private FontStyle fontStyle;

    public StyleText(final String text) {
        this.text = text;
        face = StringUtils.EMPTY;
        colorHexCode = ColorUtil.toHexCode(0, 0, 0);
        fontStyle = FontStyle.NORMAL;
        size = StringUtils.EMPTY;
    }

    public StyleText(final String text, final String face, final String colorHexCode, final String size, final FontStyle fontStyle) {
        this.text = text;
        this.face = face;
        this.colorHexCode = colorHexCode;
        this.fontStyle = fontStyle;
        this.size = size;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public String getColorHexCode() {
        return colorHexCode;
    }

    public void setColorHexCode(final String colorHexCode) {
        this.colorHexCode = colorHexCode;
    }

    public FontStyle getFontStyle() {
        return fontStyle;
    }

    public void setFontStyle(final FontStyle fontStyle) {
        this.fontStyle = fontStyle;
    }

    public String getFace() {
        return face;
    }

    public void setFace(final String face) {
        this.face = face;
    }

    public String getSize() {
        return size;
    }

    public void setSize(final String size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return text;
    }

}
