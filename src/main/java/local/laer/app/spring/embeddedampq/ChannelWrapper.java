/*
 * The MIT License
 *
 * Copyright 2014 Lars Eriksson (larsq.eriksson@gmail.com).
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

package local.laer.app.spring.embeddedampq;

import com.rabbitmq.client.Channel;
import java.util.Objects;

/**
 *
 * @author Lars Eriksson (larsq.eriksson@gmail.com)
 */
class ChannelWrapper implements Comparable<ChannelWrapper> {
    private final int channelNumber;

    public static ChannelWrapper wrap(Channel channel) {
        return new ChannelWrapper(channel.getChannelNumber());
    }
    
    public ChannelWrapper(int channelNumber) {
        this.channelNumber = channelNumber;
    }

    public int getChannelNumber() {
        return channelNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj == this) {
            return obj == this;
        }
        if (!getClass().equals(obj.getClass())) {
            return false;
        }
        ChannelWrapper other = getClass().cast(obj);
        return channelNumber == other.channelNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelNumber);
    }

    @Override
    public int compareTo(ChannelWrapper o) {
        return Integer.compare(channelNumber, o.channelNumber);
    }

    @Override
    public String toString() {
        return String.format("%s{%s}", getClass().getSimpleName(), channelNumber);
    }
    
}
