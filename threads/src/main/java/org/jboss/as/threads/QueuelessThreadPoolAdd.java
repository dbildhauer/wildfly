/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.threads;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.threads.ThreadsSubsystemThreadPoolOperationUtils.QueuelessThreadPoolParameters;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Adds a queueless thread pool.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class QueuelessThreadPoolAdd extends AbstractAddStepHandler {

    public static final QueuelessThreadPoolAdd BLOCKING = new QueuelessThreadPoolAdd(true, DefaultThreadFactoryProvider.STANDARD_PROVIDER);
    public static final QueuelessThreadPoolAdd NON_BLOCKING = new QueuelessThreadPoolAdd(true, DefaultThreadFactoryProvider.STANDARD_PROVIDER);

    static final AttributeDefinition[] BLOCKING_ATTRIBUTES = new AttributeDefinition[] {PoolAttributeDefinitions.KEEPALIVE_TIME,
        PoolAttributeDefinitions.MAX_THREADS, PoolAttributeDefinitions.THREAD_FACTORY};

    static final AttributeDefinition[] NON_BLOCKING_ATTRIBUTES = new AttributeDefinition[BLOCKING_ATTRIBUTES.length + 1];

    static final AttributeDefinition[] RW_ATTRIBUTES = new AttributeDefinition[] {PoolAttributeDefinitions.KEEPALIVE_TIME,
        PoolAttributeDefinitions.MAX_THREADS};

    static {
        System.arraycopy(BLOCKING_ATTRIBUTES, 0, NON_BLOCKING_ATTRIBUTES, 0, BLOCKING_ATTRIBUTES.length);
        NON_BLOCKING_ATTRIBUTES[NON_BLOCKING_ATTRIBUTES.length - 1] = PoolAttributeDefinitions.HANDOFF_EXECUTOR;
    }

    private final boolean blocking;
    private final DefaultThreadFactoryProvider defaultThreadFactoryProvider;

    public QueuelessThreadPoolAdd(boolean blocking, DefaultThreadFactoryProvider defaultThreadFactoryProvider) {
        this.blocking = blocking;
        this.defaultThreadFactoryProvider = defaultThreadFactoryProvider;
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();
        model.get(NAME).set(name);

        AttributeDefinition[] attributes = blocking ? BLOCKING_ATTRIBUTES : NON_BLOCKING_ATTRIBUTES;
        for(final AttributeDefinition attribute : attributes) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
            final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {

        final QueuelessThreadPoolParameters params = ThreadsSubsystemThreadPoolOperationUtils.parseQueuelessThreadPoolParameters(context, operation, model, blocking);

        final QueuelessThreadPoolService service = new QueuelessThreadPoolService(params.getMaxThreads(), blocking, params.getKeepAliveTime());

        ThreadsSubsystemThreadPoolOperationUtils.installThreadPoolService(service, params.getName(), params.getThreadFactory(),
                defaultThreadFactoryProvider, service.getThreadFactoryInjector(), service.getHandoffExecutorInjector(),
                params.getHandoffExecutor(), context.getServiceTarget(), newControllers, verificationHandler);
    }
}
