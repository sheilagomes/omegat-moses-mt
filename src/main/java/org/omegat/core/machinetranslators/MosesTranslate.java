/*
 * Copyright (C) 2014 Aaron Madlon-Kay <aaron@madlon-kay.com>.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package org.omegat.core.machinetranslators;

import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.htmlparser.jericho.CharacterReference;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.omegat.core.Core;
import org.omegat.tokenizer.ITokenizer;
import org.omegat.util.Language;
import org.omegat.util.Token;

/**
 * Support for Moses Server
 * 
 * @author Aaron Madlon-Kay
 */
public class MosesTranslate extends BaseTranslate {

    @Override
    protected String getPreferenceName() {
        return "allow_moses_translate";
    }

    public String getName() {
        return "Moses";
    }

    @Override
    protected String translate(Language sLang, Language tLang, String text) throws Exception {

        String server = System.getProperty("moses.server.url");
        
        if (server == null) {
            return "The Moses server address is not available.";
        }

        Map<String, String> mosesParams = new HashMap<String, String>();
        
        XmlRpcClient client = new XmlRpcClient();
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        
        config.setServerURL(new URL(server));
        client.setConfig(config);
        
        mosesParams.put("align", "true");
        mosesParams.put("report-all-factors", "true");
        mosesParams.put("text", mosesPreprocess(text, sLang.getLocale()));
        
        Object[] xmlRpcParams = new Object[] { mosesParams };

        String result;  
        try {
            @SuppressWarnings("rawtypes")
            HashMap response = (HashMap) client.execute("translate", xmlRpcParams);
            result = (String) response.get("text");
	    } catch (XmlRpcException e) {
            return e.getLocalizedMessage();
        }

        return mosesPostprocess(result, tLang);
    }
    
    private String mosesPreprocess(String text, Locale locale) {
        ITokenizer tokenizer = Core.getProject().getSourceTokenizer();
        StringBuilder sb = new StringBuilder();
        for (Token t : tokenizer.tokenizeAllExactly(text)) {
            sb.append(t.getTextFromString(text));
            sb.append(" ");
        }
        String result = sb.toString();
        return result.toLowerCase(locale);
    }
    
    private String mosesPostprocess(String text, Language targetLanguage) {
        String result = CharacterReference.decode(text);
        
        result = DeNormalize.processSingleLine(result).replaceAll("\\s+", " ").trim();
        
        result = result.replaceAll("(\\|UNK)+", "");
        
        if (!targetLanguage.isSpaceDelimited()) {
            result = result.replaceAll("(?<=[\u3001-\u9fa0])\\s+(?=[\u3001-\u9fa0])", "");
        }

        return cleanSpacesAroundTags(result, text);
    }
}
