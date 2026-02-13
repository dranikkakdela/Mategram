<img width="1280" height="640" alt="Cover" src="https://github.com/user-attachments/assets/1796a4a7-0ddc-4dd1-8410-e0743ee6f663" />

## Mategram

A modern Telegram client built with Jetpack Compose, Kotlin, and Material 3 Design, offering a sleek and intuitive user experience. Designed for speed, efficiency, and seamless interaction, it leverages the power of Kotlin to deliver a smooth and responsive interface. Whether messaging, sharing media, or managing chats, this client is crafted to enhance your communication experience with the latest Android technologies.

Made with Kotlin and love ❤️

> **Note:** I am just a novice in coding, so the code may contain errors or non-optimal solutions. Contributions, suggestions, and bug reports are very welcome!


## Build

To build the project, you need to set up your environment:

1.  **Configure `local.properties`**
    Define your Telegram API credentials in the `local.properties` file located in the root directory:
    ```properties
    TELEGRAM_API_ID=123456
    TELEGRAM_API_HASH="your_api_hash"
    ```

2.  **Google Services**
    Place your `google-services.json` file in the `mategram/mategram` directory.
	
3.	**Build third-party dependencies**
	Run `build_vpx.sh` and `build_ffmpeg.sh` scripts in the `material/jni/src/main/cpp` directory
	*Note for Windows: you need to use Cygwin/MSYS2 to build it*


## Contacts

**Dev:** Artem Batinkin ([@xxcactussell](https://xxcactussell.com))

**News:** [Mategram Telegram Channel](https://t.me/mategram_client)

## License

Copyright (C) 2025–2026 Artem Batinkin (xxcactussell)

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the [GNU General Public License](LICENSE) for more details.
