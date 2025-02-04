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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fortify.util.mapdb.CustomSerializerElsa;

import lombok.Getter;

@Getter
public final class Artifact implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final CustomSerializerElsa<Artifact> SERIALIZER = new CustomSerializerElsa<>(Artifact.class);
	@JsonProperty private ArtifactLocation location;
	// @JsonProperty private Integer parentIndex;
	// @JsonProperty private Integer offset;
	// @JsonProperty private Integer length;
	// @JsonProperty private String[] roles;
	// @JsonProperty private String mimeType;
	// @JsonProperty private ArtifactContent contents;
	// @JsonProperty private String encoding;
	// @JsonProperty private String sourceLanguage;
	// @JsonProperty private Map<String, String> hashes;
	// @JsonProperty private Date lastModifiedTimeUtc;
	// @JsonProperty private String description;
}