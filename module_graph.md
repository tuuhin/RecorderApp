# Module Graph

```mermaid
%%{
  init: {
    'theme': 'base'
  }
}%%

graph TB
  :app["app"]
  subgraph :core
    :core:ui["ui"]
    :core:ui["ui"]
    :core:utils["utils"]
  end
  subgraph :testing
    :testing:runtime["runtime"]
  end
  subgraph :data
    :data:categories["categories"]
    :data:database["database"]
    :data:recordings["recordings"]
    :data:datastore["datastore"]
    :data:recorder["recorder"]
    :data:use_case["use_case"]
    :data:location["location"]
    :data:editor["editor"]
    :data:player["player"]
    :data:worker["worker"]
    :data:worker["worker"]
    :data:bookmarks["bookmarks"]
    :data:interactions["interactions"]
    :data:interactions["interactions"]
    :data:player["player"]
    :data:bookmarks["bookmarks"]
    :data:editor["editor"]
    :data:recorder["recorder"]
    :data:location["location"]
    :data:use_case["use_case"]
    :data:categories["categories"]
    :data:database["database"]
    :data:recordings["recordings"]
    :data:datastore["datastore"]
  end
  subgraph :feature
    :feature:settings["settings"]
    :feature:widget["widget"]
    :feature:recorder["recorder"]
    :feature:player["player"]
    :feature:player-shared["player-shared"]
    :feature:player-shared["player-shared"]
    :feature:onboarding["onboarding"]
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
    :feature:onboarding["onboarding"]
  end

  :core:ui --> :testing:runtime
  :data:categories --> :testing:runtime
  :data:categories --> :core:ui
  :data:categories --> :core:utils
  :data:categories --> :data:database
  :feature:settings --> :testing:runtime
  :feature:settings --> :core:ui
  :feature:settings --> :core:utils
  :feature:settings --> :data:recordings
  :feature:settings --> :data:datastore
  :feature:widget --> :testing:runtime
  :feature:widget --> :core:utils
  :feature:widget --> :core:ui
  :feature:widget --> :data:recorder
  :feature:widget --> :data:recordings
  :feature:widget --> :data:use_case
  :data:location --> :testing:runtime
  :data:location --> :core:utils
  :data:location --> :data:datastore
  :data:editor --> :testing:runtime
  :data:editor --> :core:utils
  :data:editor --> :data:player
  :data:editor --> :data:recordings
  :data:editor --> :data:worker
  :data:editor --> :data:datastore
  :feature:recorder --> :testing:runtime
  :feature:recorder --> :core:ui
  :feature:recorder --> :core:utils
  :feature:recorder --> :data:recorder
  :data:worker --> :testing:runtime
  :data:worker --> :core:utils
  :data:worker --> :core:ui
  :data:worker --> :data:recordings
  :feature:player --> :testing:runtime
  :feature:player --> :core:ui
  :feature:player --> :core:utils
  :feature:player --> :data:player
  :feature:player --> :data:bookmarks
  :feature:player --> :data:recordings
  :feature:player --> :data:interactions
  :feature:player --> :feature:player-shared
  :data:interactions --> :testing:runtime
  :data:interactions --> :core:utils
  :data:interactions --> :data:bookmarks
  :data:interactions --> :data:recordings
  :data:player --> :testing:runtime
  :data:player --> :core:utils
  :data:player --> :data:recordings
  :data:player --> :data:datastore
  :data:bookmarks --> :testing:runtime
  :data:bookmarks --> :core:utils
  :data:bookmarks --> :data:recordings
  :data:bookmarks --> :data:database
  :feature:player-shared --> :testing:runtime
  :feature:player-shared --> :core:ui
  :feature:player-shared --> :core:utils
  :feature:player-shared --> :data:player
  :feature:player-shared --> :data:editor
  :feature:player-shared --> :data:recordings
  :feature:player-shared --> :data:interactions
  :feature:player-shared --> :data:use_case
  :data:recorder --> :testing:runtime
  :data:recorder --> :core:utils
  :data:recorder --> :data:use_case
  :data:recorder --> :data:location
  :data:recorder --> :data:datastore
  :data:recorder --> :data:recordings
  :data:recorder --> :data:bookmarks
  :data:use_case --> :testing:runtime
  :data:use_case --> :core:utils
  :data:use_case --> :data:interactions
  :data:use_case --> :data:recordings
  :data:use_case --> :data:datastore
  :data:use_case --> :data:categories
  :feature:onboarding --> :testing:runtime
  :feature:onboarding --> :core:ui
  :feature:onboarding --> :core:utils
  :feature:onboarding --> :data:datastore
  :feature:categories --> :testing:runtime
  :feature:categories --> :core:ui
  :feature:categories --> :core:utils
  :feature:categories --> :data:categories
  :feature:categories --> :data:recordings
  :data:database --> :testing:runtime
  :data:database --> :core:utils
  :feature:editor --> :testing:runtime
  :feature:editor --> :core:ui
  :feature:editor --> :core:utils
  :feature:editor --> :data:editor
  :feature:editor --> :data:player
  :feature:editor --> :data:recordings
  :feature:editor --> :feature:player-shared
  :feature:recordings --> :testing:runtime
  :feature:recordings --> :core:ui
  :feature:recordings --> :core:utils
  :feature:recordings --> :data:categories
  :feature:recordings --> :data:recordings
  :feature:recordings --> :data:use_case
  :feature:recordings --> :data:interactions
  :feature:recordings --> :feature:categories
  :data:recordings --> :testing:runtime
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
  :app --> :feature:onboarding
  :data:datastore --> :testing:runtime
  :data:datastore --> :core:utils

classDef android-library fill:#4169E1,stroke:#fff,stroke-width:2px,color:#fff;
classDef kotlin-jvm fill:#720e9e,stroke:#fff,stroke-width:2px,color:#fff;
classDef android-application fill:#98FB98,stroke:#fff,stroke-width:2px,color:#fff;
class :core:ui android-library
class :testing:runtime android-library
class :data:categories android-library
class :core:utils kotlin-jvm
class :data:database android-library
class :feature:settings android-library
class :data:recordings android-library
class :data:datastore android-library
class :feature:widget android-library
class :data:recorder android-library
class :data:use_case android-library
class :data:location android-library
class :data:editor android-library
class :data:player android-library
class :data:worker android-library
class :feature:recorder android-library
class :feature:player android-library
class :data:bookmarks android-library
class :data:interactions android-library
class :feature:player-shared android-library
class :feature:onboarding android-library
class :feature:categories android-library
class :feature:editor android-library
class :feature:recordings android-library
class :app android-application

```