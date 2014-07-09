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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import java.util.Objects;
import java.util.Optional;

/**
 * Internal representation of a message. Structure is immutable. This is used
 * for storing the message that cannot be delivered at once.
 * @author Lars Eriksson (larsq.eriksson@gmail.com)
 */
class Message implements Comparable<Message> {
    private final Envelope envelope;
    private final byte[] payload;
    private final AMQP.BasicProperties basicProperties;

    Message(Envelope envelope, byte[] payload, AMQP.BasicProperties basicProperties) {
        this.envelope = envelope;
        this.payload = payload;
        this.basicProperties = basicProperties;
    }

    public AMQP.BasicProperties getBasicProperties() {
        return basicProperties;
    }

    public Envelope getEnvelope() {
        return envelope;
    }

    public byte[] getPayload() {
        return payload;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null || obj == this) {
            return obj == this;
        }
        
        if(!Objects.equals(getClass(), obj.getClass())) {
            return false;
        }
        
        Message other = getClass().cast(obj);
        
        return Objects.equals(deliveryTag(), other.deliveryTag());
    }

    @Override
    public int hashCode() {
        return Objects.hash(deliveryTag());
    }
    
    private Long deliveryTag() {
        return Optional.ofNullable(envelope).map(Envelope::getDeliveryTag).orElse(null);
    }
    
    @Override
    public int compareTo(Message o) {
        return Long.compare(deliveryTag(), o.deliveryTag());
    }
}
