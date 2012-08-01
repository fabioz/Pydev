
package com.rc.retroweaver.event;

/**
 * A callback interface to indicate weaving status.
 *
 * @author rreyelts
 * Donated by Sean Shubin
 */
public interface WeaveListener {
  void weavingPath( String sourcePath );
}

