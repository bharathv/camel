/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.jms;

import java.util.ArrayList;
import java.util.List;

import javax.jms.ConnectionFactory;
import javax.jms.Session;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JmsComponentTransactedCacheLevelNameTest extends AbstractJMSTest {

    protected final String componentName = "activemq";

    private final List<Session> sessions = new ArrayList<>();

    @Override
    protected JmsComponent setupComponent(
            CamelContext camelContext, ConnectionFactory connectionFactory, String componentName) {
        JmsComponent jms = super.setupComponent(camelContext, connectionFactory, componentName);
        jms.setCacheLevelName("CACHE_CONSUMER");
        jms.setTransacted(true);
        return jms;
    }

    @Test
    public void testJmsCacheLevelName() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(2);

        template.sendBody("activemq:queue:foo", "Hello World");
        template.sendBody("activemq:queue:foo", "Bye World");

        MockEndpoint.assertIsSatisfied(context);

        Assertions.assertEquals(2, sessions.size());
        // should be same session as it is cached
        Assertions.assertSame(sessions.get(0), sessions.get(1), "Should cache and reuse JMS session");
    }

    @Override
    public String getComponentName() {
        return componentName;
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("activemq:queue:foo?transacted=true")
                        .process(exchange -> {
                            JmsMessage jms = exchange.getIn(JmsMessage.class);
                            assertNotNull(jms);
                            Session session = jms.getJmsSession();
                            assertNotNull(session, "Should have JMS session");
                            log.debug("{}", session);
                            sessions.add(session);
                        })
                        .to("mock:result");
            }
        };
    }
}
