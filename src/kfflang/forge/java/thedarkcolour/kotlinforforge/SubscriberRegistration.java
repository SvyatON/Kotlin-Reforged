package thedarkcolour.kotlinforforge;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.eventbus.api.listener.ObjBooleanBiConsumer;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.eventbus.internal.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.unsafe.UnsafeHacks;

final class SubscriberRegistration {
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void register(BusGroup modGroup, String target, Object listener) {
        boolean staticOnly = listener instanceof Class<?>;
        Class<?> type = staticOnly ? (Class<?>) listener : listener.getClass();
        Object receiver = staticOnly ? null : listener;
        for (Method method : type.getMethods()) {
            SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
            if (annotation == null || method.isSynthetic() || staticOnly && !Modifier.isStatic(method.getModifiers())) continue;
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length < 1 || parameters.length > 2 || !Event.class.isAssignableFrom(parameters[0])) throw new IllegalArgumentException("Invalid event handler: " + method);
            Class eventType = parameters[0];
            boolean modEvent = IModBusEvent.class.isAssignableFrom(eventType);
            BusGroup group = switch (target) {
                case "BOTH" -> modEvent ? modGroup : BusGroup.DEFAULT;
                case "MOD" -> modGroup;
                case "FORGE" -> BusGroup.DEFAULT;
                default -> throw new IllegalArgumentException("Unknown event bus: " + target);
            };
            if ((group == BusGroup.DEFAULT) == modEvent) throw new IllegalArgumentException("Event handler is on the wrong bus: " + method);
            boolean cancellable = Cancellable.class.isAssignableFrom(eventType);
            if (annotation.priority() == Priority.MONITOR && (method.getReturnType() == boolean.class || annotation.alwaysCancelling())) throw new IllegalArgumentException("Monitoring listeners cannot cancel events: " + method);
            UnsafeHacks.setAccessible(method);
            if (parameters.length == 2) {
                if (!cancellable || parameters[1] != boolean.class || annotation.priority() != Priority.MONITOR || method.getReturnType() != void.class || annotation.alwaysCancelling()) throw new IllegalArgumentException("Invalid cancellation monitor: " + method);
                CancellableEventBus.create(group, eventType).addListener((ObjBooleanBiConsumer) (event, cancelled) -> invoke(method, receiver, event, cancelled));
            } else if (method.getReturnType() == boolean.class) {
                if (!cancellable || annotation.alwaysCancelling()) throw new IllegalArgumentException("Invalid cancelling handler: " + method);
                CancellableEventBus.create(group, eventType).addListener(annotation.priority(), (Predicate) event -> (boolean) invoke(method, receiver, event));
            } else {
                if (method.getReturnType() != void.class || annotation.alwaysCancelling() && !cancellable) throw new IllegalArgumentException("Invalid event handler return type: " + method);
                Consumer callback = event -> invoke(method, receiver, event);
                if (annotation.alwaysCancelling()) CancellableEventBus.create(group, eventType).addListener(annotation.priority(), true, callback);
                else EventBus.create(group, eventType).addListener(annotation.priority(), callback);
            }
        }
    }

    private static Object invoke(Method method, Object receiver, Object... arguments) {
        try {
            return method.invoke(receiver, arguments);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException failure) {
            throw new RuntimeException(failure);
        }
    }
}
