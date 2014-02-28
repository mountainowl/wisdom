package org.wisdom.api.bodies;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;

import org.wisdom.api.http.Context;
import org.wisdom.api.http.MimeTypes;
import org.wisdom.api.http.Renderable;
import org.wisdom.api.http.Result;

/**
 * A renderable object taking a String as parameter.
 */
public class RenderableString implements Renderable<String> {

    //TODO Support encoding

    private final String rendered;
    private final String type;

    public RenderableString(String object) {
        this(object, null);
    }

    public RenderableString(StringBuilder object) {
        this(object.toString(), null);
    }

    public RenderableString(StringBuffer object) {
        this(object.toString(), null);
    }

    public RenderableString(Object object) {
        this(object.toString(), null);
    }

    public RenderableString(Object object, String type) {
        this(object.toString(), type);
    }

    public RenderableString(StringWriter object) {
        this(object.toString(), null);
    }

    public RenderableString(String object, String type) {
        rendered = object;
        this.type = type;
    }

    @Override
    public InputStream render(Context context, Result result) throws Exception {
    	byte[] bytes = null;

    	if(result != null){ // We have a result, charset have to be provided
    		if(result.getCharset() == null){ // No charset provided
    			result.with(Charset.defaultCharset()); // Set the default encoding
    		}
    		bytes = rendered.getBytes(result.getCharset());
    	}else{
    		//No Result, use the default platform encoding
    		bytes = rendered.getBytes();
    	}

        return new ByteArrayInputStream(bytes);
    }

    @Override
    public long length() {
        return rendered.length();
    }

    @Override
    public String mimetype() {
        if (type == null) {
            return MimeTypes.HTML;
        } else {
            return type;
        }
    }

    @Override
    public String content() {
        return rendered;
    }

    @Override
    public boolean requireSerializer() {
        return false;
    }

    @Override
    public void setSerializedForm(String serialized) {
        // Nothing because serialization is not supported for this renderable class.
    }

    @Override
    public boolean mustBeChunked() {
        return false;
    }

}
