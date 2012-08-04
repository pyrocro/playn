/**
 * Copyright 2012 The PlayN Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package playn.core;

import pythagoras.f.Point;

/**
 * Handles the common logic for all platform {@link Mouse} implementations.
 */
public abstract class MouseImpl implements Mouse {

  private boolean enabled = true;
  private Listener listener;
  private AbstractLayer activeLayer;
  private AbstractLayer hoverLayer;

  @Override
  public boolean hasMouse() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void setListener(Listener listener) {
    this.listener = listener;
  }

  @Override
  public void lock() {
    // noop
  }

  @Override
  public void unlock() {
    // noop
  }

  @Override
  public boolean isLocked() {
    return false;
  }

  @Override
  public boolean isLockSupported() {
    return false;
  }

  protected boolean onMouseDown(ButtonEvent.Impl event) {
    if (!enabled)
      return false;

    boolean preventDefault = false;
    if (listener != null) {
      event.setPreventDefault(preventDefault);
      listener.onMouseDown(event);
      preventDefault = event.getPreventDefault();
    }

    GroupLayer root = PlayN.graphics().rootLayer();
    if (root.interactive()) {
      Point p = new Point(event.x(), event.y());
      root.transform().inverseTransform(p, p);
      p.x += root.originX();
      p.y += root.originY();
      activeLayer = (AbstractLayer)root.hitTest(p);
      if (activeLayer != null) {
        final ButtonEvent.Impl localEvent = event.localize(activeLayer);
        localEvent.setPreventDefault(preventDefault);
        activeLayer.interact(LayerListener.class, new AbstractLayer.Interaction<LayerListener>() {
          public void interact(LayerListener l) {
            l.onMouseDown(localEvent);
          }
        });
        preventDefault = localEvent.getPreventDefault();
      }
    }
    return preventDefault;
  }

  protected boolean onMouseMove(MotionEvent.Impl event) {
    if (!enabled)
      return false;

    boolean preventDefault = false;
    if (listener != null) {
      event.setPreventDefault(preventDefault);
      listener.onMouseMove(event);
      preventDefault = event.getPreventDefault();
    }

    GroupLayer root = PlayN.graphics().rootLayer();
    if (root.interactive()) {
      Point p = new Point(event.x(), event.y());
      root.transform().inverseTransform(p, p);
      p.x += root.originX();
      p.y += root.originY();
      AbstractLayer lastHoverLayer = hoverLayer;
      hoverLayer = (AbstractLayer)root.hitTest(p);

      // handle onMouseDrag if we have an active layer, onMouseMove otherwise
      if (activeLayer != null) {
        preventDefault = dispatchMotion(event, preventDefault, activeLayer, ON_MOUSE_DRAG);
      } else if (hoverLayer != null) {
        preventDefault = dispatchMotion(event, preventDefault, hoverLayer, ON_MOUSE_MOVE);
      }

      // handle onMouseOut
      if (lastHoverLayer != hoverLayer && lastHoverLayer != null) {
        preventDefault = dispatchMotion(event, preventDefault, lastHoverLayer, ON_MOUSE_OUT);
      }

      // handle onMouseOver
      if (hoverLayer != lastHoverLayer && hoverLayer != null) {
        preventDefault = dispatchMotion(event, preventDefault, hoverLayer, ON_MOUSE_OVER);
      }
    }

    return preventDefault;
  }

  protected boolean onMouseUp(ButtonEvent.Impl event) {
    if (!enabled)
      return false;

    boolean preventDefault = false;
    if (listener != null) {
      event.setPreventDefault(preventDefault);
      listener.onMouseUp(event);
      preventDefault = event.getPreventDefault();
    }

    if (activeLayer != null) {
      final ButtonEvent.Impl localEvent = event.localize(activeLayer);
      localEvent.setPreventDefault(preventDefault);
      activeLayer.interact(LayerListener.class, new AbstractLayer.Interaction<LayerListener>() {
        public void interact(LayerListener l) {
          l.onMouseUp(localEvent);
        }
      });
      preventDefault = localEvent.getPreventDefault();
      activeLayer = null;
    }

    return preventDefault;
  }

  protected boolean onMouseWheelScroll(final WheelEvent.Impl event) {
    if (!enabled)
      return false;

    event.setPreventDefault(false);
    if (listener != null)
      listener.onMouseWheelScroll(event);

    AbstractLayer target = (activeLayer != null) ? activeLayer : hoverLayer;
    if (target != null)
      target.interact(LayerListener.class, new AbstractLayer.Interaction<LayerListener>() {
        public void interact(LayerListener l) {
          l.onMouseWheelScroll(event);
        }
      });
    return event.getPreventDefault();
  }

  protected boolean dispatchMotion(MotionEvent.Impl event, boolean preventDefault,
                                   AbstractLayer layer, final Dispatcher dispatcher) {
    final MotionEvent.Impl localEvent = event.localize(layer);
    localEvent.setPreventDefault(preventDefault);
    layer.interact(LayerListener.class, new AbstractLayer.Interaction<LayerListener>() {
      public void interact(LayerListener l) {
        dispatcher.dispatch(l, localEvent);
      }
    });
    return localEvent.getPreventDefault();
  }

  protected interface Dispatcher {
    void dispatch(LayerListener l, MotionEvent.Impl event);
  }
  protected static final Dispatcher ON_MOUSE_DRAG = new Dispatcher() {
    public void dispatch(LayerListener l, MotionEvent.Impl event) {
      l.onMouseDrag(event);
    }
  };
  protected static final Dispatcher ON_MOUSE_MOVE = new Dispatcher() {
    public void dispatch(LayerListener l, MotionEvent.Impl event) {
      l.onMouseMove(event);
    }
  };
  protected static final Dispatcher ON_MOUSE_OVER = new Dispatcher() {
    public void dispatch(LayerListener l, MotionEvent.Impl event) {
      l.onMouseOver(event);
    }
  };
  protected static final Dispatcher ON_MOUSE_OUT = new Dispatcher() {
    public void dispatch(LayerListener l, MotionEvent.Impl event) {
      l.onMouseOut(event);
    }
  };
}
