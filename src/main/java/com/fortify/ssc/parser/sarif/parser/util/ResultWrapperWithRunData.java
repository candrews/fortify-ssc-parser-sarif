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
package com.fortify.ssc.parser.sarif.parser.util;

import java.text.MessageFormat;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.fortify.plugin.api.BasicVulnerabilityBuilder.Priority;
import com.fortify.ssc.parser.sarif.domain.ArtifactLocation;
import com.fortify.ssc.parser.sarif.domain.Level;
import com.fortify.ssc.parser.sarif.domain.Location;
import com.fortify.ssc.parser.sarif.domain.Message;
import com.fortify.ssc.parser.sarif.domain.MultiformatMessageString;
import com.fortify.ssc.parser.sarif.domain.ReportingDescriptor;
import com.fortify.ssc.parser.sarif.domain.Result;
import com.fortify.ssc.parser.sarif.domain.Result.Kind;

import lombok.experimental.Delegate;

/**
 * This {@link Result} wrapper class provides access to the configured
 * {@link Result} methods, but adds various utility methods that 
 * utilize information provided by the configured {@link RunData}
 * object.
 * 
 * @author Ruud Senden
 *
 */
public class ResultWrapperWithRunData {
	@Delegate private final Result result;
	@Delegate private final RunData runData;
	private volatile ReportingDescriptor rule;
	
	public ResultWrapperWithRunData(Result result, RunData runData) {
		this.result = result;
		this.runData = runData;
	}
	
	public String resolveFullFileName(final String defaultValue) {
		String value = defaultValue;
		final Map<String,ArtifactLocation> originalUriBaseIds = runData.getOriginalUriBaseIds();
		Location[] locations = getLocations();
		if ( locations!=null && locations.length>0 && locations[0].getPhysicalLocation()!=null ) {
			value = locations[0].getPhysicalLocation().resolveArtifactLocation(runData).getFullFileName(originalUriBaseIds);
		} else if ( getAnalysisTarget()!=null ) {
			value = getAnalysisTarget().getFullFileName(originalUriBaseIds);
		}
		return value;
	}
	
	public ReportingDescriptor resolveRule() {
		if ( this.rule == null ) {
			this.rule = resolveRuleByIndex();
			if ( this.rule == null ) {
				this.rule = resolveRuleById();
				if ( this.rule == null ) {
					this.rule = resolveRuleByGuid();
				}
			}
		}
		return this.rule;
	}
	
	private ReportingDescriptor resolveRuleByIndex() {
		Integer ruleIndex = resolveRuleIndex();
		return ruleIndex==null ? null : runData.getRuleByIndex(ruleIndex);
	}
	
	private Integer resolveRuleIndex() {
		if ( result.getRuleIndex()!=null ) { 
			return result.getRuleIndex(); 
		} else if ( result.getRule()!=null && result.getRule().getIndex()!=null ) {
			return result.getRule().getIndex();
		}
		return null;
	}
	
	private ReportingDescriptor resolveRuleById() {
		String ruleId = resolveRuleId();
		return ruleId==null ? null : runData.getRuleById(ruleId);
	}
	
	private String resolveRuleId() {
		if ( StringUtils.isNotBlank(result.getRuleId()) ) { 
			return result.getRuleId(); 
		} else if ( result.getRule()!=null && StringUtils.isNotBlank(result.getRule().getId()) ) {
			return result.getRule().getId();
		}
		return null;
	}
	
	private ReportingDescriptor resolveRuleByGuid() {
		String ruleGuid = result.getRule()==null ? null : result.getRule().getGuid(); 
		return ruleGuid==null ? null : runData.getRuleByGuid(ruleGuid);
	}

	public Level resolveLevel() {
		Level level = result.getLevel();
		ReportingDescriptor rule = resolveRule();
		// TODO Currently we don't check for level overrides;
		//      See https://docs.oasis-open.org/sarif/sarif/v2.1.0/cs01/sarif-v2.1.0-cs01.html#_Toc16012604 
		if ( level == null ) {
			if ( rule!=null ) {
				level = rule.getDefaultConfiguration().getLevel();
			} else if ( getKind()==Kind.fail || getKind()==null ) {
				level = Level.warning;
			} else {
				level = Level.none;
			}
		}
		return level;
	}
	
	public String getResultMessage() {
		Message msg = getMessage();
		if ( msg.getText()!=null ) {
			return msg.getText();
		} else if ( msg.getId()!=null ) {
			MultiformatMessageString msgString = getMultiformatMessageStringForId(msg.getId());
			// TODO Do we need to improve handling of single quotes around arguments?
			// TODO Should we throw an exception if msgString==null, instead of just returning null?
			return msgString==null ? null : MessageFormat.format(msgString.getText().replace("'", "''"), (Object[])msg.getArguments());
		} else {
			return null;
		}
	}

	private MultiformatMessageString getMultiformatMessageStringForId(String id) {
		ReportingDescriptor rule = resolveRule();
		Map<String, MultiformatMessageString> messageStrings = rule==null ? null : rule.getMessageStrings();
		return messageStrings==null ? null : messageStrings.get(id);
	}
	
	public String getVulnerabilityId() {
		if ( StringUtils.isNotBlank(getGuid()) ) {
			return getGuid();
		} else if ( StringUtils.isNotBlank(getCorrelationGuid()) ) {
			return getCorrelationGuid();
		} else {
			return getCalculatedIdString();
		}
	}
	
	// TODO This may need improvement
	//      As described at https://docs.oasis-open.org/sarif/sarif/v2.1.0/cs01/sarif-v2.1.0-cs01.html#_Toc16012888
	//      we calculate a unique id string based on tool name, full file location,
	//      rule id and partial finger prints if available. To increase chances
	//      of generating a unique id, we also include the result message.
	//      However, this could potentially still result in duplicate id strings. 
	//      Possibly we could add information from other properties like region, 
	//      logical location or code flows, but these may either not be available, or 
	//      still result in duplicate uuid strings.
	private String getCalculatedIdString() {
		if ( getFingerprints()!=null && getFingerprints().size()>0 ) {
			return new TreeMap<>(getFingerprints()).toString();
		} else {
			String partialFingerPrints = getPartialFingerprints()==null?"":new TreeMap<>(getPartialFingerprints()).toString();
			return String.join("|", 
				runData.getToolName(),
				resolveFullFileName("Unknown"),
				getRuleId(),
				partialFingerPrints,
				getResultMessage());
		}
	}
	
	public String getCategory() {
		ReportingDescriptor rule = resolveRule();
		if ( rule != null ) {
			if ( StringUtils.isNotBlank(rule.getName()) ) {
				return StringUtils.capitalize(StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(rule.getName()), StringUtils.SPACE));
			} else if ( rule.getProperties()!=null && rule.getProperties().containsKey("Type") ) {
				return rule.getProperties().get("Type").toString();
			}
		} else {
			String ruleId = resolveRuleId();
			if ( StringUtils.isNotBlank(ruleId) ) {
				return ruleId;
			} 
		}
		return runData.getEngineType();
	}
	
	public String getSubCategory() {
		return null;
	}

	public Priority resolvePriority() {
		if ( "Fortify".equalsIgnoreCase(runData.getToolName()) && result.getProperties()!=null && result.getProperties().containsKey("priority")) {
			return Priority.valueOf(result.getProperties().get("priority").toString());
		} else {
			return resolveLevel().getFortifyPriority();
		}
	}
	
	public String resolveKingdom() {
		if ( result.getProperties()!=null && result.getProperties().containsKey("kingdom") ) {
			return result.getProperties().get("kingdom").toString();
		} else {
			ReportingDescriptor rule = resolveRule();
			if ( rule!=null && rule.getProperties()!=null && rule.getProperties().containsKey("Kingdom") ) {
				return rule.getProperties().get("Kingdom").toString();
			}
		}
		return null;
	}
	
	
}
