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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.mail.Address;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

public final class MockMailbox {

    private static final Map<Address, MockMailbox> mailboxes =
        new ConcurrentHashMap<>();

    public static MockMailbox get(Address a) {
        return mailboxes.computeIfAbsent(a, MockMailbox::new);
    }

    public static MockMailbox get(String address) throws AddressException {
        return get(new InternetAddress(address));
    }

    public static void resetAll() {
        mailboxes.clear();
    }

    private final Address address;
    private final MailboxFolder inbox;

    private final MailboxFolder root = new MailboxFolder("", this, true);

    private MockMailbox(final Address address) {
        this.address = address;
        inbox = root.addSpecialSubFolder("INBOX");
    }

    /**
     * @return the address
     */
    public Address getAddress() {
        return address;
    }

    public MailboxFolder getInbox() {
        return inbox;
    }

    public MailboxFolder getRoot() {
        return root;
    }

    static boolean isInbox(String name) {
        return "inbox".equalsIgnoreCase(name);
    }

}
