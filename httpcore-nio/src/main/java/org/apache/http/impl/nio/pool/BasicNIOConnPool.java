/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.apache.http.impl.nio.pool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.HttpHost;
import org.apache.http.annotation.ThreadSafe;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.pool.AbstractNIOConnPool;
import org.apache.http.nio.pool.NIOConnFactory;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.pool.PoolEntry;

/**
 * Basic non-blocking {@link NHttpClientConnection} pool.
 * <p>
 * The following parameters can be used to customize the behavior of this
 * class:
 * <ul>
 *  <li>{@link org.apache.http.params.CoreProtocolPNames#HTTP_ELEMENT_CHARSET}</li>
 *  <li>{@link org.apache.http.params.CoreConnectionPNames#SO_TIMEOUT}</li>
 *  <li>{@link org.apache.http.params.CoreConnectionPNames#CONNECTION_TIMEOUT}</li>
 *  <li>{@link org.apache.http.params.CoreConnectionPNames#SOCKET_BUFFER_SIZE}</li>
 *  <li>{@link org.apache.http.params.CoreConnectionPNames#MAX_HEADER_COUNT}</li>
 *  <li>{@link org.apache.http.params.CoreConnectionPNames#MAX_LINE_LENGTH}</li>
 * </ul>
 *
 * @since 4.2
 */
@ThreadSafe
public class BasicNIOConnPool extends AbstractNIOConnPool<HttpHost, NHttpClientConnection,
                                            PoolEntry<HttpHost, NHttpClientConnection>> {

    private static AtomicLong COUNTER = new AtomicLong();

    private final HttpParams params;

    public BasicNIOConnPool(
            final ConnectingIOReactor ioreactor,
            final NIOConnFactory<HttpHost, NHttpClientConnection> connFactory,
            final HttpParams params) {
        super(ioreactor, connFactory, 2, 20);
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        this.params = params;
    }

    public BasicNIOConnPool(
            final ConnectingIOReactor ioreactor, final HttpParams params) {
        this(ioreactor, new BasicNIOConnFactory(params), params);
    }

    @Override
    protected SocketAddress resolveRemoteAddress(final HttpHost host) {
        return new InetSocketAddress(host.getHostName(), host.getPort());
    }

    @Override
    protected SocketAddress resolveLocalAddress(final HttpHost host) {
        return null;
    }

    @Override
    protected BasicNIOPoolEntry createEntry(final HttpHost host, final NHttpClientConnection conn) {
        return new BasicNIOPoolEntry(Long.toString(COUNTER.getAndIncrement()), host, conn);
    }

    @Override
    protected void closeEntry(final PoolEntry<HttpHost, NHttpClientConnection> entry) {
        NHttpClientConnection conn = entry.getConnection();
        try {
            conn.shutdown();
        } catch (IOException ex) {
        }
    }

    @Override
    public Future<PoolEntry<HttpHost, NHttpClientConnection>> lease(
            final HttpHost route,
            final Object state,
            final FutureCallback<PoolEntry<HttpHost, NHttpClientConnection>> callback) {
        int connectTimeout = HttpConnectionParams.getConnectionTimeout(this.params);
        return super.lease(route, state, connectTimeout, TimeUnit.MILLISECONDS, callback);
    }

    @Override
    public Future<PoolEntry<HttpHost, NHttpClientConnection>> lease(
            final HttpHost route,
            final Object state) {
        int connectTimeout = HttpConnectionParams.getConnectionTimeout(this.params);
        return super.lease(route, state, connectTimeout, TimeUnit.MILLISECONDS, null);
    }

}
