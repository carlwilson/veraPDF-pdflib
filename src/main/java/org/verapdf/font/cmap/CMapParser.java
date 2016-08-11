package org.verapdf.font.cmap;

import org.apache.log4j.Logger;
import org.verapdf.as.io.ASInputStream;
import org.verapdf.parser.BaseParser;
import org.verapdf.parser.Token;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class parses CMap files and constructs CMap objects.
 *
 * @author Sergey Shemyakov
 */
public class CMapParser extends BaseParser {

    private static final Logger LOGGER = Logger.getLogger(CMapParser.class);

    private CMap cMap;

    /**
     * {@inheritDoc}
     */
    public CMapParser(String fileName) throws FileNotFoundException {
        super(fileName);
        cMap = new CMap();
    }

    /**
     * {@inheritDoc}
     */
    public CMapParser(InputStream fileStream) throws IOException {
        super(fileStream);
        cMap = new CMap();
    }

    /**
     * {@inheritDoc}
     */
    public CMapParser(ASInputStream asInputStream) throws IOException {
        super(asInputStream);
        cMap = new CMap();
    }

    /**
     * @return constructed CMap.
     */
    public CMap getCMap() {
        return cMap;
    }

    /**
     * Method parses CMap from given source.
     */
    public void parse() throws IOException {
        initializeToken();
        //Skipping starting comments
        skipSpaces(true);
        while (getToken().type != Token.Type.TT_EOF) {
            nextToken();
            processToken();
        }
    }

    private void processToken() throws IOException {
        switch (getToken().type) {
            case TT_NAME:
                switch (getToken().getValue()) {
                    case "WMode":
                        skipSpaces();
                        readNumber();
                        this.cMap.setwMode((int) getToken().integer);
                        break;
                    case "Registry":
                        nextToken();
                        if (getToken().type.equals(Token.Type.TT_LITSTRING)) {
                            this.cMap.setRegistry(getToken().getValue());
                        } else {
                            throw new IOException("CMap contains invalid /" + getToken().getValue() + " value");
                        }
                        break;
                    case "Ordering":
                        nextToken();
                        if (getToken().type.equals(Token.Type.TT_LITSTRING)) {
                            this.cMap.setOrdering(getToken().getValue());
                        } else {
                            throw new IOException("CMap contains invalid /" + getToken().getValue() + " value");
                        }
                        break;
                    case "CMapName":
                        nextToken();
                        if (getToken().type.equals(Token.Type.TT_NAME)) {
                            this.cMap.setName(getToken().getValue());
                        } else {
                            throw new IOException("CMap contains invalid /" + getToken().getValue() + " value");
                        }
                        break;
                }
                break;
            case TT_INTEGER:
                int listLength = (int) getToken().integer;
                nextToken();
                if (getToken().type != Token.Type.TT_KEYWORD) {
                    break;
                }
                processList(listLength, getToken().getValue());
        }
    }

    private void processList(int listLength, String type) throws IOException {
        if (type.startsWith("begin")) {
            type = type.substring(5); //skipping leading "begin"
        }
        for (int i = 0; i < listLength; ++i) {
            switch (type) {
                case "codespacerange":
                    readLineCodeSpaceRange();
                    break;
                case "cidrange":
                    readLineCIDRange();
                    break;
                case "notdefrange":
                    readLineNotDefRange();
                    break;
                case "cidchar":
                    readSingleCharMapping();
                    break;
                case "notdefchar":
                    readSingleNotDefMapping();
                    break;
                //TODO: add here unicode mappings reading if needed
            }
        }
        nextToken();
        if (!getToken().getValue().equals("end" + type)) {
            LOGGER.warn("Unexpected end of " + type + " in CMap");
        }
    }

    private void readLineCodeSpaceRange() throws IOException {
        nextToken();
        checkTokenType(Token.Type.TT_HEXSTRING, "codespacerange list");
        byte[] begin = getRawBytes(getToken().getValue());

        nextToken();
        checkTokenType(Token.Type.TT_HEXSTRING, "codespacerange list");
        byte[] end = getRawBytes(getToken().getValue());

        CodeSpace codeSpace = new CodeSpace(begin, end);

        boolean overlaps = false;
        for (CodeSpace cs : cMap.getCodeSpaces()) {
            if (cs.overlaps(codeSpace)) {
                overlaps = true;
                break;
            }
        }
        if (!overlaps) {
            cMap.getCodeSpaces().add(codeSpace);
            if (begin.length < cMap.shortestCodeSpaceLength) {
                cMap.shortestCodeSpaceLength = begin.length;
            }
        } else {
            LOGGER.debug("CMap " + cMap.getName() + " has overlapping codespace ranges.");
        }
    }

    private void readLineCIDRange() throws IOException {
        nextToken();
        checkTokenType(Token.Type.TT_HEXSTRING, "cidrange list");
        long cidRangeStart = numberFromBytes(getRawBytes(getToken().getValue()));

        nextToken();
        checkTokenType(Token.Type.TT_HEXSTRING, "cidrange list");
        long cidRangeEnd = numberFromBytes(getRawBytes(getToken().getValue()));

        nextToken();
        checkTokenType(Token.Type.TT_INTEGER, "cidrange list");
        this.cMap.addCidInterval(new CIDInterval((int) cidRangeStart,
                (int) cidRangeEnd, (int) getToken().integer));
    }

    private void readLineNotDefRange() throws IOException {
        nextToken();
        checkTokenType(Token.Type.TT_HEXSTRING, "notdef list");
        long notDefRangeStart = numberFromBytes(getRawBytes(getToken().getValue()));

        nextToken();
        checkTokenType(Token.Type.TT_HEXSTRING, "notdef list");
        long notDefRangeEnd = numberFromBytes(getRawBytes(getToken().getValue()));

        nextToken();
        checkTokenType(Token.Type.TT_INTEGER, "notdef list");
        this.cMap.addNotDefInterval(new NotDefInterval((int) notDefRangeStart,
                (int) notDefRangeEnd, (int) getToken().integer));
    }

    private void readSingleCharMapping() throws IOException {
        nextToken();
        checkTokenType(Token.Type.TT_HEXSTRING, "cidchar");
        long charCode = numberFromBytes(getRawBytes(getToken().getValue()));

        nextToken();
        checkTokenType(Token.Type.TT_INTEGER, "cidchar");
        this.cMap.addSingleCidMapping(new SingleCIDMapping((int) charCode,
                (int) getToken().integer));
    }

    private void readSingleNotDefMapping() throws IOException {
        nextToken();
        checkTokenType(Token.Type.TT_HEXSTRING, "notdefchar");
        long notDefCharCode = numberFromBytes(getRawBytes(getToken().getValue()));

        nextToken();
        checkTokenType(Token.Type.TT_INTEGER, "notdefchar");
        this.cMap.addSingleNotDefMapping(new SingleCIDMapping((int) notDefCharCode,
                (int) getToken().integer));
    }

    static long numberFromBytes(byte[] num) {
        long res = 0;
        for (int i = 0; i < num.length; ++i) {
            res += (num[i] & 0x00FF) << ((num.length - i - 1) * 8);
        }
        return res;
    }

    private void checkTokenType(Token.Type type, String where) throws IOException {
        if (getToken().type != type) {
            throw new IOException("CMap contains invalid entry in " + where +
                    ". Expected " + type + " but got " + getToken().type);
        }
    }
}
