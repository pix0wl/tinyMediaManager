/*
 * Copyright 2012 - 2013 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager;

import java.io.File;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.movie.Movie;
import org.tinymediamanager.core.movie.MovieList;

/**
 * The class UpdateTasks. To perform needed update tasks
 * 
 * @author Manuel Laggner
 */
public class UpgradeTasks {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeTasks.class);

  public static void performUpgradeTasksBeforeDatabaseLoading(String oldVersion) {
    String v = "" + oldVersion;

    if (StringUtils.isBlank(v)) {
      LOGGER.info("Performing upgrade tasks to version 2.0");
      // upgrade from alpha/beta to "TV Show" 2.0 format
      // happens only once
      JOptionPane.showMessageDialog(null,
          "And since you are upgrading to a complete new version, we need to cleanup/delete the complete database this time.\nWe're sorry for that.");
      FileUtils.deleteQuietly(new File(Constants.DB));

      // upgrade from alpha - delete unneeded files
      FileUtils.deleteQuietly(new File("lib/jackson-core-lgpl.jar"));
      FileUtils.deleteQuietly(new File("lib/jackson-core-lgpl.jarv"));
      FileUtils.deleteQuietly(new File("lib/jackson-mapper-lgpl.jar"));
      FileUtils.deleteQuietly(new File("lib/jackson-mapper-lgpl.jarv"));

      // check really old alpha version
      FileUtils.deleteQuietly(new File("lib/beansbinding-1.2.1.jar"));
      FileUtils.deleteQuietly(new File("lib/beansbinding.jar"));
      v = "2.0"; // set version for other updates
    }
  }

  public static void performUpgradeTasksAfterDatabaseLoading(String oldVersion) {
    MovieList movieList = MovieList.getInstance();
    String v = "" + oldVersion;

    if (StringUtils.isBlank(v)) {
      v = "2.0"; // set version for other updates
    }

    if (compareVersion("2.5", v) > 0) {
      // upgrade tasks for movies; added with 2.5;
      Globals.entityManager.getTransaction().begin();
      for (Movie movie : movieList.getMovies()) {
        movie.findActorImages();
        movie.saveToDb();
      }
      Globals.entityManager.getTransaction().commit();
    }

    if (compareVersion("2.5.2", v) > 0) {
      // clean tmdb id
      Globals.entityManager.getTransaction().begin();
      for (Movie movie : movieList.getMovies()) {
        if (movie.getId("tmdbId") != null && movie.getId("tmdbId") instanceof String) {
          try {
            Integer tmdbid = Integer.parseInt((String) movie.getId("tmdbId"));
            movie.setId("tmdbId", tmdbid);
            movie.saveToDb();
          }
          catch (Exception e) {
          }
        }
      }
      Globals.entityManager.getTransaction().commit();
    }
  }

  /**
   * compares the given version (v1) against another one (v2)
   * 
   * @param v1
   * @param v2
   * @return < 0 if v1 is lower, > 0 if v1 is higher, == 0 if equal
   */
  private static int compareVersion(String v1, String v2) {
    String s1 = normalisedVersion(v1);
    String s2 = normalisedVersion(v2);
    return s1.compareTo(s2);
  }

  private static String normalisedVersion(String version) {
    return normalisedVersion(version, ".", 4);
  }

  private static String normalisedVersion(String version, String sep, int maxWidth) {
    String[] split = Pattern.compile(sep, Pattern.LITERAL).split(version);
    StringBuilder sb = new StringBuilder();
    for (String s : split) {
      sb.append(String.format("%" + maxWidth + 's', s));
    }
    return sb.toString();
  }
}