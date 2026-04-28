package com.stanlemon.healthy.hangar;

import java.util.List;
import java.util.Optional;

/** Stores and retrieves paper planes stowed in the hangar. Implementations must be thread-safe. */
public interface HangarService {

  /**
   * Stow a new paper plane and return the persisted record with a generated id and stowed
   * timestamp.
   */
  PaperPlane stow(PaperPlaneRequest request);

  /** Look up a previously stowed plane by id. */
  Optional<PaperPlane> find(String id);

  /** Return all stowed planes, most recently stowed first. */
  List<PaperPlane> listAll();

  /** Number of planes currently stowed. */
  int count();
}
