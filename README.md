# :studio_microphone: RecorderApp

An android audio recorder app, designed to simplify the process of capturing and managing
recordings.With a clean and intuitive interface, this app offers a seamless user experience.

## 💁 About

A fully functioned recorder app with an ability to record over multiple media codec like **acc**,*
*amr**,**opus** over different quality. The app can continue its recording in the background, so
you
never miss a moment. Once the recording, you can easily manage your files within the app.
There `built-in player`, you can listen to your recordings directly within the app or use the
convenient media notification for playback control.
The apps also features creating `category` for the recordings the categories help to keep the
recording organized, you can also add `bookmarks` to the portion of the recording.

### :building_construction: Features

What are the features this app can provide, here's some:

- :musical_keyboard: **Effortless Recording**: Start recording instantly with a single tap of a
  button.
- :chart_with_upwards_trend: **Visualization**: Watch the amplitude levels fluctuate in real-time as
  you record or play the media.
- :loop: **Background Recording**: Keep recording even when you switch to other apps or lock
  your device.
- :bellhop_bell: **Convenient Notifications**: Control your recordings directly from
  notifications,without having to return to the app.
- :file_cabinet: **File Management**: Organize, delete, share, or rename your own recordings with
  ease, on api level `api-31`+ you can also read other apps recordings.
- :package: **Category Management**: Categories your recording into different category, so that you
  can easily find the required one.
- :record_button: **Built-in Player**: Listen to your recordings directly within the app, complete
  with a media notification for easy playback control.
- :bookmark: **Bookmarks** : You can add multiple bookmarks with the recording to easily remember
  the important portions of your recording. You can too export the bookmarks as csv file.
- :eight_spoked_asterisk: **Widgets And Shortcuts** : App comes with two `widgets` and few
  `shortcuts` to ease the experience of the user.

## 📷 Screenshots

These are some screen shorts for the app

<p align="center">

   <img src="./screenshots/recorder_base_framed.png" width="24%" />
   <img src="./screenshots/recordings_framed.png" width="24%" />
   <img src="./screenshots/player_base_framed.png" width="24%" />
   <img src="./screenshots/app_settings_framed.png" width="24%" />
   <img src="./screenshots/recording_categories_framed.png" width="24%" />
   <img src="./screenshots/player_bookmarks_framed.png" width="24%" />
   <img src="./screenshots/app_widget_preview_framed.png" width="24%" />
</p>

For more [screenshots](./screenshots).

## :safety_pin: Permissions

Basically, Android is a bit of a control freak when it comes to apps. It's all about keeping your
phone safe and sound.Here are the list of permission required in this app

- :microphone: **Record Audio** : Use to record voices and other sounds
- :musical_note: **Music and Audio Access** : Use to save and read the recordings
- :bell: **Notifications** : Yes you can control the recorder from the notification

There are some optional permissions, but they aren't necessary to the core audio recording and
playing stuff.

- :telephone_receiver: **Phone State** : To handle incomming calls during a recording.
- :world_map: **Location** : Some mediacodec like `acc` and `three_gpp` can add a additional
  location data with the recording.You can view this location data on other devices which can read
  metadata.

## :hammer_and_wrench: Getting Started

Here are the steps to get started with this app:

1. **Clone the Repository:**

   ```bash
   git clone https://github.com/tuuhin/RecorderApp.git
   ```

2. **Open Project**
   Open the project in android studio

3. **Build and Run**
   Build and run on android device with api 29 and above

You have your app running this is just simple as that.

### :woman_cook: Contributing

Contributions are always welcomed from the community

- Fork the repository.
- Create your feature branch (git checkout -b feature/YourFeature).
- Commit your changes (git commit -am 'Add some feature').
- Push to the branch (git push origin feature/YourFeature).
- Create a new Pull Request.

### :curly_loop: Feedback and Support

Yes, there maybe some issues regarding the app or some unwanted scenario or any new feature that can
be added to the app. Please add
an [issue](https://github.com/tuuhin/RecorderApp/issues) if you have one.

### :next_track_button: What's next

The app development is complete for now, with the core features fully implemented. Although the
`edit` feature has not yet been rolled out, a significant amount of time and effort has been
invested in this project. For the time being, it is considered completed, and further enhancements
will be addressed as needed in the future.
