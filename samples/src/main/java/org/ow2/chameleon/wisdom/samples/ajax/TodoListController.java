package org.ow2.chameleon.wisdom.samples.ajax;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.felix.ipojo.annotations.Requires;
import org.ow2.chameleon.wisdom.api.DefaultController;
import org.ow2.chameleon.wisdom.api.annotations.*;
import org.ow2.chameleon.wisdom.api.http.HttpMethod;
import org.ow2.chameleon.wisdom.api.http.Result;
import org.ow2.chameleon.wisdom.api.router.Router;
import org.ow2.chameleon.wisdom.api.templates.Template;

import java.util.List;

/**
 * A simple controller to manage a todo list (in memory).
 */
@Controller
public class TodoListController extends DefaultController {

    @View("ajax/index")
    private Template index;
    @Requires
    private Router router;
    private List<Task> items = Lists.newArrayList();

    @Route(method = HttpMethod.GET, uri = "/todo")
    public Result index() {
        return ok(render(index));
    }

    @Route(method = HttpMethod.GET, uri = "/todo/tasks")
    public Result retrieve() {
        return ok(items).json();
    }

    @Route(method = HttpMethod.DELETE, uri = "/todo/tasks/{id}")
    public Result delete(@Parameter("id") int id) {
        removeTaskById(id);
        return ok();
    }

    @Route(method = HttpMethod.POST, uri = "/todo/tasks")
    public Result create(@Attribute("name") String name) {
        Task task = new Task(name);
        task.setUpdateUrl(router.getReverseRouteFor(this, "update", "id", task.id));
        task.setDeleteUrl(router.getReverseRouteFor(this, "delete", "id", task.id));
        items.add(task);
        return ok(task).json();
    }

    @Route(method = HttpMethod.POST, uri = "/todo/tasks/{id}")
    public Result update(@Parameter("id") int id, @Attribute("completed") boolean completed) {
        Task task = getTaskById(id);
        if (task == null) {
            return notFound().render(ImmutableMap.<String, String>of("message", "Task " + context().parameterFromPath
                    ("id") + " not found")).json();
        } else {
            task.completed(completed);
            if (completed) {
                return ok().render(ImmutableMap.<String, String>of("message", "Task " + context().parameterFromPath
                        ("id") + " completed")).json();
            } else {
                return ok().render(ImmutableMap.<String, String>of("message", "Task " + context().parameterFromPath
                        ("id") + " uncompleted")).json();
            }
        }
    }

    private Task getTaskById(int id) {
        for (Task t : items) {
            if (t.id == id) {
                return t;
            }
        }
        return null;
    }

    private Task getTaskById(String id) {
        return getTaskById(Integer.parseInt(id));
    }

    private void removeTaskById(int id) {
        Task t = getTaskById(id);
        if (t != null) {
            items.remove(t);
        }
    }


}
