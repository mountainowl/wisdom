package org.wisdom.router;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.wisdom.api.http.HttpMethod;
import org.wisdom.api.router.Route;
import org.wisdom.api.router.RouteBuilder;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Test the router implementation
 */
public class RouterTest {

    RequestRouter router = new RequestRouter();

    @Test
    public void simpleRoute() throws Exception {
        FakeController controller = new FakeController();
        controller.setRoutes(ImmutableList.of(
                new RouteBuilder().route(HttpMethod.GET).on("/foo").to(controller, "foo")
        ));
        router.bindController(controller);

        assertThat(router.getRouteFor(HttpMethod.GET, "/foo")).isNotNull();
        assertThat(router.getRouteFor(HttpMethod.GET, "/foo").getControllerObject()).isEqualTo(controller);
    }

    @Test
    public void missingRoute() throws Exception {
        FakeController controller = new FakeController();
        controller.setRoutes(ImmutableList.of(
                new RouteBuilder().route(HttpMethod.GET).on("/foo").to(controller, "foo")
        ));
        router.bindController(controller);

        assertThat(router.getRouteFor(HttpMethod.GET, "/bar").isUnbound()).isTrue();
        assertThat(router.getRouteFor(HttpMethod.GET, "/foo").getControllerObject()).isEqualTo(controller);
    }

    @Test
    public void routeMissingBecauseOfBadMethod() throws Exception {
        FakeController controller = new FakeController();
        controller.setRoutes(ImmutableList.of(
                new RouteBuilder().route(HttpMethod.GET).on("/foo").to(controller, "foo")
        ));
        router.bindController(controller);

        assertThat(router.getRouteFor(HttpMethod.PUT, "/foo").isUnbound()).isTrue();
        assertThat(router.getRouteFor(HttpMethod.DELETE, "/foo").isUnbound()).isTrue();
        assertThat(router.getRouteFor(HttpMethod.POST, "/foo").isUnbound()).isTrue();
    }

    @Test
    public void routeWithPathParameter() throws Exception {
        FakeController controller = new FakeController();
        controller.setRoutes(ImmutableList.of(
                new RouteBuilder().route(HttpMethod.GET).on("/foo/{id}").to(controller, "foo")
        ));
        router.bindController(controller);

        assertThat(router.getRouteFor(HttpMethod.GET, "/foo").isUnbound()).isTrue();
        assertThat(router.getRouteFor(HttpMethod.GET, "/foo/").isUnbound()).isTrue();
        Route route = router.getRouteFor(HttpMethod.GET, "/foo/test");
        assertThat(route.isUnbound()).isFalse();
        assertThat(route.getPathParametersEncoded("/foo/test").get("id")).isEqualToIgnoringCase("test");
    }

    @Test
    public void routeWithTwoPathParameters() throws Exception {
        FakeController controller = new FakeController();
        controller.setRoutes(ImmutableList.of(
                new RouteBuilder().route(HttpMethod.GET).on("/foo/{id}/{email}").to(controller, "foo")
        ));
        router.bindController(controller);

        Route route = router.getRouteFor(HttpMethod.GET, "/foo/1234/foo@aol.com");
        assertThat(route).isNotNull();
        assertThat(route.getPathParametersEncoded("/foo/1234/foo@aol.com").get("id")).isEqualToIgnoringCase("1234");
        assertThat(route.getPathParametersEncoded("/foo/1234/foo@aol.com").get("email")).isEqualToIgnoringCase
                ("foo@aol.com");
    }

    @Test
    public void subRoute() throws Exception {
        FakeController controller = new FakeController();
        controller.setRoutes(ImmutableList.of(
                new RouteBuilder().route(HttpMethod.GET).on("/foo/*").to(controller, "foo")
        ));
        router.bindController(controller);

        assertThat(router.getRouteFor(HttpMethod.GET, "/foo").isUnbound()).isTrue();
        assertThat(router.getRouteFor(HttpMethod.GET, "/foo/bar")).isNotNull();
        assertThat(router.getRouteFor(HttpMethod.GET, "/foo/bar").getControllerObject()).isEqualTo(controller);
        assertThat(router.getRouteFor(HttpMethod.GET, "/foo/bar/baz")).isNotNull();
        assertThat(router.getRouteFor(HttpMethod.GET, "/foo/bar/baz").getControllerObject()).isEqualTo(controller);
    }

    @Test
    public void subRouteAsParameter() throws Exception {
        FakeController controller = new FakeController();
        controller.setRoutes(ImmutableList.of(
                new RouteBuilder().route(HttpMethod.GET).on("/foo/{path+}").to(controller, "foo")
        ));
        router.bindController(controller);

        assertThat(router.getRouteFor(HttpMethod.GET, "/foo").isUnbound()).isTrue();
        assertThat(router.getRouteFor(HttpMethod.GET, "/foo/").isUnbound()).isTrue();
        assertThat(router.getRouteFor(HttpMethod.GET, "/foo/bar").isUnbound()).isFalse();
        assertThat(router.getRouteFor(HttpMethod.GET, "/foo/bar").getControllerObject()).isEqualTo(controller);

        Route route = router.getRouteFor(HttpMethod.GET, "/foo/bar/baz");
        assertThat(route).isNotNull();
        assertThat(route.getControllerObject()).isEqualTo(controller);

        assertThat(route.getPathParametersEncoded("/foo/bar/baz").get("path")).isEqualToIgnoringCase("bar/baz");

    }

    @Test
    public void unbindTest() {
        FakeController controller = new FakeController();
        controller.setRoutes(ImmutableList.of(
                new RouteBuilder().route(HttpMethod.GET).on("/foo/{path+}").to(controller, "foo")
        ));
        router.bindController(controller);

        assertThat(router.getRouteFor(HttpMethod.GET, "/foo/bar").isUnbound()).isFalse();

        router.unbindController(controller);
        assertThat(router.getRouteFor(HttpMethod.GET, "/foo/bar").isUnbound()).isTrue();

    }


}
