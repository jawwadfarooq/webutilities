/*
 * Copyright 2010-2014 Rajendra Patil
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.webutilities.test;

import com.googlecode.webutilities.test.util.TestUtils;
import com.googlecode.webutilities.util.Utils;
import com.mockrunner.mock.web.WebMockObjectFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.googlecode.webutilities.common.Constants.HTTP_ACCEPT_ENCODING_HEADER;
import static com.googlecode.webutilities.common.Constants.HTTP_USER_AGENT_HEADER;

public abstract class AbstractWebComponentTest {

    protected Properties properties;

    protected int currentTestNumber;

    protected WebMockObjectFactory webMockObjectFactory;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWebComponentTest.class.getName());

    protected abstract String getTestPropertiesName();

    public AbstractWebComponentTest() {
        currentTestNumber = 1;
        webMockObjectFactory = new WebMockObjectFactory();
        properties = new Properties();
        String name = getTestPropertiesName();
        try {
            properties.load(this.getClass().getResourceAsStream(name));
        } catch (IOException e) {
            LOGGER.error("Failed to load props: " + name);
        }
    }

    protected abstract void setupInitParam(String name, String value);

    protected void setupResource(String resource) {
        LOGGER.debug("Setting resource : {}", resource);
        webMockObjectFactory.getMockServletContext()
                .setResourceAsStream(resource, this.getClass().getResourceAsStream(resource));
        webMockObjectFactory.getMockServletContext()
                .setRealPath(resource, this.getClass().getResource(resource).getPath());
    }

    protected void setupContextPath(String contextPath) {
        webMockObjectFactory.getMockRequest().setContextPath(contextPath);
    }

    protected void setupURI(String uri) {
        webMockObjectFactory.getMockRequest().setRequestURI(uri);
    }

    protected void setupRequestParameter(String name, String value) {
        LOGGER.debug("Setting request param : {}={}", name, value);
        webMockObjectFactory.getMockRequest().setupAddParameter(name, value);
    }

    public void setupInitParams() {
        String value = properties.getProperty(this.currentTestNumber + ".test.init.params");
        if (value != null && !value.trim().equals("")) {
            String[] params = value.split(",");
            for (String param : params) {
                String[] keyAndValue = param.split(":");
                setupInitParam(keyAndValue[0], keyAndValue[1]);
            }
        }
    }

    public void setupResources() {
        String resourcesString = properties.getProperty(this.currentTestNumber + ".test.resources");
        if (resourcesString != null && !resourcesString.trim().equals("")) {
            String[] resources = resourcesString.split(",");
            for (String resource : resources) {
                setupResource(resource);
            }
        }
    }

    public void setupRequest() {
        String contextPath = properties.getProperty(this.currentTestNumber + ".test.request.contextPath");
        setupContextPath(contextPath);

        String requestURI = properties.getProperty(this.currentTestNumber + ".test.request.uri");
        if (requestURI != null && !requestURI.trim().equals("")) {
            String[] uriAndQuery = requestURI.split("\\?");
            setupURI(uriAndQuery[0]);
            if (uriAndQuery.length > 1) {
                String[] params = uriAndQuery[1].split("&");
                webMockObjectFactory.getMockRequest().setQueryString(uriAndQuery[1]);
                for (String param : params) {
                    String[] nameValue = param.split("=");
                    setupRequestParameter(nameValue[0], nameValue[1]);
                }
            }
        }

        String userAgent = properties.getProperty(this.currentTestNumber + ".test.request.userAgent");
        if (userAgent != null && !userAgent.trim().equals("")) {
            webMockObjectFactory.getMockRequest().addHeader(HTTP_USER_AGENT_HEADER, userAgent);
        }
        String accept = properties.getProperty(this.currentTestNumber + ".test.request.accept");
        if (accept != null && !accept.trim().equals("")) {
            webMockObjectFactory.getMockRequest().addHeader(HTTP_ACCEPT_ENCODING_HEADER, accept);
        }
        //headers
        String headers = properties.getProperty(this.currentTestNumber + ".test.request.headers");
        if (headers != null && !headers.trim().equals("")) {
            String[] headersString = headers.split("&");
            for (String header : headersString) {
                String[] nameValue = header.split("=");
                if (nameValue.length == 2 && nameValue[1].contains("hashOf")) {
                    String res = nameValue[1].replaceAll(".*hashOf\\s*\\((.*)\\).*", "$1");
                    nameValue[1] = Utils.buildETagForResource(res, webMockObjectFactory.getMockServletContext());
                } else if (nameValue.length == 2 && nameValue[1].contains("lastModifiedOf")) {
                    String res = nameValue[1].replaceAll(".*lastModifiedOf\\s*\\((.*)\\)", "$1");
                    nameValue[1] = Utils.forHeaderDate(new File(webMockObjectFactory.getMockServletContext().getRealPath(res)).lastModified());
                }
                webMockObjectFactory.getMockRequest().addHeader(nameValue[0], nameValue[1]);
            }
        }
    }

    public int getExpectedStatus(int defaultStatus) throws Exception {
        return Utils.readInt(properties.getProperty(this.currentTestNumber + ".test.expected.status"), defaultStatus);
    }

    public Map<String, String> getExpectedHeaders() throws Exception {
        Map<String, String> headersMap = new HashMap<String, String>();
        String expectedHeaders = properties.getProperty(this.currentTestNumber + ".test.expected.headers");
        if (expectedHeaders == null || expectedHeaders.trim().equals("")) return headersMap;

        String[] headersString = expectedHeaders.split(",");
        for (String header : headersString) {
            String[] nameValue = header.split("=");
            if (nameValue.length == 2 && nameValue[1].contains("hashOf")) {
                String res = nameValue[1].replaceAll(".*hashOf\\s*\\((.*)\\)", "$1");
                nameValue[1] = Utils.buildETagForResource(res, webMockObjectFactory.getMockServletContext());
            } else if (nameValue.length == 2 && nameValue[1].contains("lastModifiedOf")) {
                String res = nameValue[1].replaceAll(".*lastModifiedOf\\s*\\((.*)\\)", "$1");
                nameValue[1] = Utils.forHeaderDate(new File(webMockObjectFactory.getMockServletContext().getRealPath(res)).lastModified());
            }
            headersMap.put(nameValue[0], nameValue.length == 2 ? nameValue[1] : null);
        }
        return headersMap;
    }

    public String getExpectedEncoding() {
        return getExpectedProperty("encoding");
    }

    public String getExpectedProperty(String property) {
        return properties.getProperty(this.currentTestNumber + ".test.expected." + property);
    }

    public String getExpectedOutput() throws Exception {

        String expectedResource = properties.getProperty(this.currentTestNumber + ".test.expected.output");
        if (expectedResource == null || expectedResource.trim().equals("")) return null;
        return TestUtils.readContents(this.getClass().getResourceAsStream(expectedResource), webMockObjectFactory.getMockResponse().getCharacterEncoding());
    }

    public void pre() throws Exception {

        webMockObjectFactory = new WebMockObjectFactory();

        this.initModule();

        this.setupInitParams();

        this.setupResources();

        this.prepare();

        this.setupRequest();

    }

    protected abstract void initModule();

    protected abstract void prepare() throws Exception;

    public void post() {
        this.currentTestNumber++;
    }

    public abstract void executeCurrentTestLogic() throws Exception;

    @Test
    public void testAllDefinedScenarios() throws Exception {

        while (true) {
            this.pre();

            String testCase = properties.getProperty(this.currentTestNumber + ".test.name");

            if (testCase == null || testCase.trim().equals("")) {
                break;
                //return; // no more test cases in properties file.
            }

            LOGGER.info("##################################################################################################################");
            LOGGER.debug("Test {} : {}", this.currentTestNumber, testCase);


            this.executeCurrentTestLogic();


            LOGGER.info("Test {} : PASS. {}", this.currentTestNumber, testCase);
            LOGGER.info("##################################################################################################################");

            this.post();

        }

    }
}
