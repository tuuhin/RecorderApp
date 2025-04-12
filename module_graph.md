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
    :data:recorder["recorder"]
    :data:use_case["use_case"]
    :data:location["location"]
    :data:datastore["datastore"]
    :data:bookmarks["bookmarks"]
    :data:categories["categories"]
    :data:recorder["recorder"]
    :data:location["location"]
    :data:use_case["use_case"]
    :data:interactions["interactions"]
    :data:categories["categories"]
    :data:worker["worker"]
    :data:player["player"]
    :data:database["database"]
    :data:recordings["recordings"]
    :data:worker["worker"]
    :data:datastore["datastore"]
    :data:interactions["interactions"]
    :data:player["player"]
  end
  subgraph :core
    :core:utils["utils"]
    :core:ui["ui"]
  end
  subgraph :feature
    :feature:settings["settings"]
    :feature:widget["widget"]
    :feature:recorder["recorder"]
    :feature:player["player"]
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
  :feature:recorder --> :core:ui
  :feature:recorder --> :core:utils
  :feature:recorder --> :data:recorder
  :data:use_case --> :core:utils
  :data:use_case --> :data:interactions
  :data:use_case --> :data:recordings
  :data:use_case --> :data:datastore
  :data:use_case --> :data:categories
  :data:worker --> :core:utils
  :data:worker --> :data:recordings
  :feature:player --> :core:ui
  :feature:player --> :core:utils
  :feature:player --> :data:player
  :feature:player --> :data:bookmarks
  :feature:player --> :data:recordings
  :feature:player --> :data:interactions
  :feature:player --> :data:use_case
  :feature:categories --> :core:ui
  :feature:categories --> :core:utils
  :feature:categories --> :data:categories
  :feature:categories --> :data:recordings
  :data:database --> :core:utils
  :feature:editor --> :core:ui
  :feature:editor --> :core:utils
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
class :data:recorder android-library
class :data:use_case android-library
class :data:location android-library
class :data:datastore android-library
class :data:categories android-library
class :core:ui android-library
class :feature:settings android-library
class :feature:widget android-library
class :feature:recorder android-library
class :data:interactions android-library
class :data:worker android-library
class :feature:player android-library
class :data:player android-library
class :feature:categories android-library
class :feature:editor android-library
class :feature:recordings android-library
class :app android-application

```