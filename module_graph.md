# Module Graph

```mermaid
%%{
  init: {
    'theme': 'base'
  }
}%%

graph TB
  :app["app"]
  subgraph :data
    :data:bookmarks["bookmarks"]
    :data:recordings["recordings"]
    :data:database["database"]
    :data:player["player"]
    :data:editor["editor"]
    :data:interactions["interactions"]
    :data:use_case["use_case"]
    :data:recorder["recorder"]
    :data:location["location"]
    :data:datastore["datastore"]
    :data:bookmarks["bookmarks"]
    :data:categories["categories"]
    :data:recorder["recorder"]
    :data:location["location"]
    :data:editor["editor"]
    :data:worker["worker"]
    :data:use_case["use_case"]
    :data:categories["categories"]
    :data:worker["worker"]
    :data:database["database"]
    :data:recordings["recordings"]
    :data:datastore["datastore"]
    :data:interactions["interactions"]
    :data:player["player"]
  end
  subgraph :core
    :core:utils["utils"]
    :core:ui["ui"]
  end
  subgraph :feature
    :feature:player-shared["player-shared"]
    :feature:settings["settings"]
    :feature:widget["widget"]
    :feature:recorder["recorder"]
    :feature:player["player"]
    :feature:player-shared["player-shared"]
    :feature:categories["categories"]
    :feature:editor["editor"]
    :feature:recordings["recordings"]
    :feature:categories["categories"]
    :feature:player["player"]
    :feature:recorder["recorder"]
    :feature:recordings["recordings"]
    :feature:editor["editor"]
    :feature:settings["settings"]
    :feature:widget["widget"]
  end

  :data:bookmarks --> :core:utils
  :data:bookmarks --> :data:recordings
  :data:bookmarks --> :data:database
  :feature:player-shared --> :core:ui
  :feature:player-shared --> :core:utils
  :feature:player-shared --> :data:player
  :feature:player-shared --> :data:editor
  :feature:player-shared --> :data:recordings
  :feature:player-shared --> :data:interactions
  :feature:player-shared --> :data:use_case
  :data:recorder --> :core:utils
  :data:recorder --> :data:use_case
  :data:recorder --> :data:location
  :data:recorder --> :data:datastore
  :data:recorder --> :data:recordings
  :data:recorder --> :data:bookmarks
  :data:categories --> :core:ui
  :data:categories --> :core:utils
  :data:categories --> :data:database
  :feature:settings --> :core:ui
  :feature:settings --> :core:utils
  :feature:settings --> :data:datastore
  :feature:widget --> :core:utils
  :feature:widget --> :core:ui
  :feature:widget --> :data:recorder
  :feature:widget --> :data:recordings
  :feature:widget --> :data:use_case
  :data:location --> :core:utils
  :data:location --> :data:datastore
  :data:editor --> :core:utils
  :data:editor --> :data:player
  :data:editor --> :data:recordings
  :data:editor --> :data:worker
  :data:editor --> :data:datastore
  :feature:recorder --> :core:ui
  :feature:recorder --> :core:utils
  :feature:recorder --> :data:recorder
  :data:use_case --> :core:utils
  :data:use_case --> :data:interactions
  :data:use_case --> :data:recordings
  :data:use_case --> :data:datastore
  :data:use_case --> :data:categories
  :data:worker --> :core:utils
  :data:worker --> :core:ui
  :data:worker --> :data:recordings
  :feature:player --> :core:ui
  :feature:player --> :core:utils
  :feature:player --> :data:player
  :feature:player --> :data:bookmarks
  :feature:player --> :data:recordings
  :feature:player --> :data:interactions
  :feature:player --> :feature:player-shared
  :feature:categories --> :core:ui
  :feature:categories --> :core:utils
  :feature:categories --> :data:categories
  :feature:categories --> :data:recordings
  :data:database --> :core:utils
  :feature:editor --> :core:ui
  :feature:editor --> :core:utils
  :feature:editor --> :data:editor
  :feature:editor --> :data:player
  :feature:editor --> :data:recordings
  :feature:editor --> :feature:player-shared
  :feature:recordings --> :core:ui
  :feature:recordings --> :core:utils
  :feature:recordings --> :data:categories
  :feature:recordings --> :data:recordings
  :feature:recordings --> :data:use_case
  :feature:recordings --> :data:interactions
  :feature:recordings --> :feature:categories
  :data:recordings --> :core:utils
  :data:recordings --> :data:database
  :data:recordings --> :data:datastore
  :data:recordings --> :data:location
  :data:recordings --> :data:categories
  :app --> :core:utils
  :app --> :core:ui
  :app --> :data:worker
  :app --> :data:interactions
  :app --> :feature:categories
  :app --> :feature:player
  :app --> :feature:recorder
  :app --> :feature:recordings
  :app --> :feature:editor
  :app --> :feature:settings
  :app --> :feature:widget
  :data:datastore --> :core:utils
  :data:interactions --> :core:utils
  :data:interactions --> :data:bookmarks
  :data:interactions --> :data:recordings
  :data:player --> :core:utils
  :data:player --> :data:recordings
  :data:player --> :data:datastore

classDef android-library fill:#4169E1,stroke:#fff,stroke-width:2px,color:#fff;
classDef kotlin-jvm fill:#720e9e,stroke:#fff,stroke-width:2px,color:#fff;
classDef android-application fill:#98FB98,stroke:#fff,stroke-width:2px,color:#fff;
class :data:bookmarks android-library
class :core:utils kotlin-jvm
class :data:recordings android-library
class :data:database android-library
class :feature:player-shared android-library
class :core:ui android-library
class :data:player android-library
class :data:editor android-library
class :data:interactions android-library
class :data:use_case android-library
class :data:recorder android-library
class :data:location android-library
class :data:datastore android-library
class :data:categories android-library
class :feature:settings android-library
class :feature:widget android-library
class :data:worker android-library
class :feature:recorder android-library
class :feature:player android-library
class :feature:categories android-library
class :feature:editor android-library
class :feature:recordings android-library
class :app android-application

```