package com.lonelystorm.air.component.services.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.TreeMap;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.apache.sling.testing.resourceresolver.MockHelper;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BsonPostProcessorImplTest {

    @Mock
    private SlingHttpServletRequest slingHttpServletRequest;

    private ResourceResolver resolver;

    private SlingPostProcessor bsonPostProcessor;

    @Before
    public void setUp() throws Exception {
        ResourceResolverFactory factory = new MockResourceResolverFactory();
        resolver = spy(factory.getResourceResolver(null));

        MockHelper.create(resolver)
            .resource("/bson")
                .p("children", new String[] { "{\"bsonId\": \"a\"}", "{\"bsonId\": \"d\"}", "invalid" })
            .resource("/bson/empty")
            .resource("/bson/a")
                .p("jcr:primaryType", "nt:unstructured")
                .p("autoGeneratedBson", true)
                .p("sling:resourceType", "foundation/components/parsys")
            .resource("/bson/b")
                .p("jcr:primaryType", "nt:unstructured")
                .p("autoGeneratedBson", true)
                .p("sling:resourceType", "foundation/components/parsys")
            .resource("/bson/c")
                .p("jcr:primaryType", "nt:unstructured")
                .p("sling:resourceType", "foundation/components/parsys")
        .commit();

        Map<String, String[]> parameters = new TreeMap<String, String[]>();
        parameters.put(":lonelystorm.air:bson@children", new String[] { "foundation/components/parsys" });
        parameters.put(":lonelystorm.air:bson@invalid", null);
        parameters.put(":lonelystorm.air:bson@empty", new String[] {});
        parameters.put("dialog", new String[] { "/apps/aem-air/test" });
        when(slingHttpServletRequest.getParameterMap()).thenReturn(parameters);

        Resource resource = resolver.getResource("/bson");
        when(slingHttpServletRequest.getResource()).thenReturn(resource);

        bsonPostProcessor = new BsonPostProcessorImpl();
    }

    @Test
    public void processExisting() throws Exception {
        reset(resolver);
        bsonPostProcessor.process(slingHttpServletRequest, null);

        assertNotNull(resolver.getResource("/bson/a"));
        assertNull(resolver.getResource("/bson/b"));
        assertNotNull(resolver.getResource("/bson/c"));
        assertNotNull(resolver.getResource("/bson/d"));
        verify(resolver, times(1)).commit();
    }

    @Test
    public void processEmpty() throws Exception {
        Resource resource = resolver.getResource("/bson/empty");
        when(slingHttpServletRequest.getResource()).thenReturn(resource);

        reset(resolver);
        bsonPostProcessor.process(slingHttpServletRequest, null);

        verify(resolver, times(1)).commit();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processCreateException() throws Exception {
        reset(resolver);
        doThrow(new PersistenceException()).when(resolver).create(any(Resource.class), any(String.class), any(Map.class));
        bsonPostProcessor.process(slingHttpServletRequest, null);

        assertNotNull(resolver.getResource("/bson/a"));
        assertNotNull(resolver.getResource("/bson/b"));
        assertNotNull(resolver.getResource("/bson/c"));
        assertNull(resolver.getResource("/bson/d"));
        verify(resolver, never()).commit();
        verify(resolver, times(1)).revert();
    }

}
