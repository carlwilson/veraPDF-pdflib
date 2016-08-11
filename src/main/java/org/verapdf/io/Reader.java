package org.verapdf.io;

import org.verapdf.cos.*;
import org.verapdf.cos.xref.COSXRefInfo;
import org.verapdf.parser.DecodedObjectStreamParser;
import org.verapdf.parser.PDFParser;
import org.verapdf.parser.XRefReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Timur Kamalov
 */
public class Reader extends XRefReader {

	private PDFParser parser;
	private COSHeader header;
	private Map<Long, DecodedObjectStreamParser> objectStreams;

	private boolean linearized;

	public Reader(final COSDocument document, final String fileName) throws IOException {
		super();
		this.parser = new PDFParser(document, fileName);
		this.objectStreams = new HashMap<>();
		this.linearized = false;
		init();
	}

	public Reader(final COSDocument document, final InputStream fileStream) throws IOException {
		super();
		this.parser = new PDFParser(document, fileStream);
		this.objectStreams = new HashMap<>();
		this.linearized = false;
		init();
	}

	//PUBLIC METHODS
	public String getHeader() {
		return this.header.getHeader();
	}

	public COSObject getObject(final COSKey key) throws IOException {
		final long offset = getOffset(key);
		if(offset > 0) {
			return getObject(offset);
		} else {
			DecodedObjectStreamParser parser = objectStreams.get(-offset);
			if(parser != null) {
				return parser.getObject(key.getNumber());
			} else {
				COSKey newKey = new COSKey(- (int)offset, 0);
				COSObject object = getObject(newKey);
				if(!object.getType().equals(COSObjType.COS_STREAM)) {
					throw new IOException("Object number " + (-offset) + " should" +
							" be object stream, but in fact it is " + object.getType());
				}
				COSStream objectStream = (COSStream) object.get();
				parser = new DecodedObjectStreamParser(
						objectStream.getData(COSStream.FilterFlags.DECODE),
						objectStream, new COSKey((int) -offset, 0));
				objectStreams.put(-offset, parser);
				return parser.getObject(key.getNumber());
			}
		}
	}

	public COSObject getObject(final long offset) throws IOException {
		return this.parser.getObject(offset);
	}

	public boolean isLinearized() {
		return this.linearized;
	}

	// PRIVATE METHODS
	private void init() throws IOException {
		this.header = this.parser.getHeader();
		this.linearized = this.parser.isLinearized();

		List<COSXRefInfo> infos = new ArrayList<COSXRefInfo>();
		this.parser.getXRefInfo(infos);
		setXRefInfo(infos);
	}

}
