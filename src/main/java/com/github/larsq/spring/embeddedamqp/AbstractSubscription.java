/*
 * The MIT License
 *
 * Copyright 2014 lars.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.larsq.spring.embeddedamqp;

import java.io.IOException;
import java.util.Objects;

/**
 * This class is the base class for subscription. A subscription denotes the 
 * tuple of the callback interface and the service tag that identifies the client.
 * This release does not support any additional properties of a subscription such
 * as no local
 * @author Lars Erikssson
 * @param <T>
 */
abstract class AbstractSubscription<T> {
    private final ChannelWrapper owner;
    private final String tag;
    private final T instance;

    AbstractSubscription(ChannelWrapper owner, String tag, T instance) {
        this.tag = tag;
        this.instance = instance;
        this.owner = owner;
    }

    abstract void onMessage(Message message) throws IOException;
    
    ChannelWrapper owner() {
        return owner;
    }
    
    String tag() {
        return tag;
    }
    
    T instance() {
        return instance;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null || obj == this) {
            return this == obj;
        }
        
        if(!getClass().equals(obj.getClass())) {
            return false;
        }
        
        AbstractSubscription other = getClass().cast(obj);
        
        return Objects.equals(tag, other.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag);
    }
}
