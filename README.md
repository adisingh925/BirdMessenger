
![Logo](https://github.com/GDSC-KIIT/BirdMessenger/blob/master/app/src/main/res/drawable/app_icon.png?raw=true)


# BirdMessenger

BirdMessenger is an instant messaging app developed using Kotlin. 

This application uses FCM (Firebase Cloud Messaging) for message delivery.

Roomdatabase for storing all the chat data.
 
Firestore for storing FCM token and profile pic (As BASE64 string).

Okhttp library is used for sending FCM payload.

Workmanager for handling network problems during sending and receiving.

Currently Available on Playstore (Open Testing).



[![APACHE LICENSE 2.0](https://upload.wikimedia.org/wikipedia/commons/thumb/7/78/Google_Play_Store_badge_EN.svg/270px-Google_Play_Store_badge_EN.svg.png?20220907104002)](https://play.google.com/store/apps/details?id=com.adreal.birdmessenger)

## Known Issues

1. Needed implmentation of retrofit for FCM.

2. POST_NOTIFICATION permission needs to be added during runtime.

3. Symmetric Encryption needs to be implemented with a suitable key exchange mechanism.

4. Minor code optimizations is needed.

5. Any suggestion for addition of new features are also appreciated.


## Authors

- [@adisingh925](https://github.com/adisingh925)


## Feedback

If you have any feedback, please reach out to us at adrealhelp@gmail.com


## License

[APACHE LICENCE 2.0](https://github.com/GDSC-KIIT/BirdMessenger/blob/master/LICENSE)


## Badges

[![APACHE LICENSE 2.0](https://img.shields.io/badge/license-Apache%202-blue)](https://choosealicense.com/licenses/mit/)



## Contributing

Contributions are always welcome!

1. You need to create a Pull Request for each known issue you fix.

2. If you find a new bug in the application you need to create a new issue on github so we can have a discussion on how to resolve it.



