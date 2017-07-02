/*
 *  JavaMail Mock3 Provider - open source mock classes for mock up JavaMail
 *  =======================================================================
 *
 *  Copyright (C) 2014 by Hendrik Saly (http://saly.de)
 *
 *  Based on ideas from Kohsuke Kawaguchi's Mock-javamail
 *  (https://java.net/projects/mock-javamail)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *  either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 */
package com.github.golovnin.javamail.mock3;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.event.ConnectionEvent;
import javax.mail.event.TransportEvent;
import javax.mail.internet.MimeMessage;

public class MockTransport extends Transport {

    private static final Address[] EMPTY = new Address[0];

    public MockTransport(Session session, URLName urlname) {
        super(session, urlname);
    }

    @Override
    public void connect(String host, int port, String user,
        String password
    ) throws MessagingException {
        if (isConnected()) {
            throw new IllegalStateException("already connected");
        }

        setConnected(true);
        notifyConnectionListeners(ConnectionEvent.OPENED);
    }

    @Override
    public void sendMessage(Message msg, Address[] addresses)
        throws MessagingException
    {
        for (Address a : addresses) {
            MockMailbox mailbox = MockMailbox.get(a);
            if (mailbox.getInbox().isSimulateError()) {

                notifyTransportListeners(TransportEvent.MESSAGE_NOT_DELIVERED,
                    EMPTY, addresses, EMPTY, msg);

                throw new MessagingException("Simulated error sending message to " + a);

            }

            mailbox.getInbox().add(new MimeMessage((MimeMessage) msg));
            notifyTransportListeners(TransportEvent.MESSAGE_DELIVERED, addresses,
                EMPTY, EMPTY, msg);
        }
    }

}
