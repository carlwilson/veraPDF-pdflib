package org.verapdf.font;

import java.io.IOException;

/**
 * Interface for all fonts in pdflib.
 *
 * @author Sergey Shemyakov
 */
public interface PDFlibFont {

    /**
     * Returns width of glyph for given character code.
     *
     * @param code is code for glyph.
     * @return width of corresponding glyph or -1 if glyph is not found.
     */
    float getWidth(int code);

    /**
     * Returns width of glyph for given glyph name.
     *
     * @param glyphName is name for glyph.
     * @return width of corresponding glyph or -1 if glyph is not found.
     */
    float getWidth(String glyphName);

    /**
     * This method does parsing of font, after it all the data needed should be
     * extracted.
     */
    void parseFont() throws IOException;
}
