package org.verapdf.font.cff;

import org.verapdf.as.io.ASInputStream;
import org.verapdf.font.CFFNumber;
import org.verapdf.font.type1.BaseCharStringParser;

import java.io.IOException;

/**
 * This class does basic parsing of Type 2 CharString to extract width value
 * from it.
 *
 * @author Sergey Shemyakov
 */
class Type2CharStringParser extends BaseCharStringParser {

    private static final int TWO_POWER_16 = 65536;

    /**
     * {@inheritDoc}
     */
    Type2CharStringParser(ASInputStream stream) throws IOException {
        super(stream);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean processNextOperator(int nextByte) throws IOException {
        switch (nextByte) {
            case 14:    // endchar
            case 19:    // cntrmask
            case 20:    // hintmask
                if (!this.stack.empty()) {
                    this.setWidth(this.stack.get(0));
                }
                break;
            case 4:     // vmoveto
            case 22:    // hmoveto
                if (this.stack.size() > 1) {
                    this.setWidth(this.stack.get(0));
                }
                break;
            case 21:    // rmoveto
                if (this.stack.size() > 2) {
                    this.setWidth(this.stack.get(0));
                }
                break;
            case 1:     // hstem
            case 3:     // vstem
            case 18:    // hstemhm
            case 23:    // vstemhm
                if (this.stack.size() % 2 == 1) {
                    this.setWidth(this.stack.get(0));
                }
                break;
            case 28:    // actually not an operator but 2-byte number
                this.stack.push(readNextNumber(nextByte));
                return false;
            default:
                break;
        }
        return true;
    }

    @Override
    protected CFFNumber readNextNumber(int firstByte) throws IOException {
        byte[] buf = new byte[4];
        if (firstByte == 28) {
            this.stream.read(buf, 2);
            return new CFFNumber((char) (((buf[0] & 0xFF) << 8) | (buf[1] & 0xFF)));
        } else {
            this.stream.read(buf, 4);
            int integer = 0;
            for (int i = 0; i < 3; ++i) {
                integer |= (buf[i] & 0xFF);
                integer <<= 8;
            }
            integer |= buf[3] & 0xFF;
            float res = integer;
            return new CFFNumber(res / TWO_POWER_16);
        }
    }
}
