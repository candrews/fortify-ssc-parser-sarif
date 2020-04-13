/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.ssc.parser.sarif.domain;

import java.io.Serializable;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public final class ArtifactLocation implements Serializable {
	private static final long serialVersionUID = 1L;
	@JsonProperty private URI uri;
	@JsonProperty private String uriBaseId;
	@JsonProperty private Integer index;
	// @JsonProperty private Message description;
	
	public URI resolveURI(Map<String, ArtifactLocation> originalUriBaseIds) {
		URI result = this.uri;
		if ( uriBaseId!=null ) {
			ArtifactLocation baseLocation = originalUriBaseIds.get(uriBaseId);
			if (baseLocation!=null) {
				URI baseUri = baseLocation.resolveURI(originalUriBaseIds);
				if ( baseUri != null ) {
					result = baseUri.resolve(result);
				}
			}
		}
		return result;
	}
	
	public String getFullFileName(Map<String, ArtifactLocation> originalUriBaseIds) {
		URI resolvedURI = resolveURI(originalUriBaseIds);
		if ( resolvedURI==null ) { return null; }
		if ( resolvedURI.getScheme()==null ) {
			//resolvedURI = URI.create("file:///").resolve(resolvedURI);
		}
		try {
			return Paths.get(resolvedURI).toString();
		} catch ( IllegalArgumentException iae ) {
			// Call above fails on Linux if resolvedURI points to an UNC path
			return resolvedURI.toString();
		}
	}
}