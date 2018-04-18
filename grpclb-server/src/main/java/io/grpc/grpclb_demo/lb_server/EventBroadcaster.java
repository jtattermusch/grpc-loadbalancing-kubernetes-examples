package io.grpc.grpclb_demo.lb_server;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

public class EventBroadcaster<T> implements EventListener<T> {

  private List<EventListener<T>> listeners = new ArrayList<>();

  public synchronized void addListener(EventListener<T> listener) {
    this.listeners.add(listener);
  }

  public synchronized void removeListener(EventListener<T> listener) {
    this.listeners.remove(listener);
  }

  @Override
  public void onUpdate(T arg) {
    for (EventListener<T> listener : snapshotListeners()) {
      listener.onUpdate(arg);
    }
  }

  private synchronized ImmutableList<EventListener<T>> snapshotListeners() {
    return ImmutableList.copyOf(listeners);
  }
}