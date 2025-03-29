# Disclaimer
I made this tool only for learning purposes and have open sourced it for educational purposes. Using this tool is very unethical and goes against discord TOS I guess. Selling this tool is strictly prohibited. I take no responsibility if Discord takes action against your account for using it for personal gain. **May discord ban you forever if you do so :)**

Also, you may find some issues with the source code as well. I think the code is not very clean and it can be improved a lot. To be honest, I didn't really focus on writing the clean code because I was only interested in turning the idea into reality. I also didn't add a way to encrypt the data before uploading it on discord, but you can modify the code and add it.

The idea of using discord as a database originates from a youtuber known as **Dev Detour** . You can find the link to that video [here](https://youtu.be/c_arQ-6ElYI?si=xsGDIHeN3wiYfP-O). 


# How this works

Basically, we will upload a file to a Discord channel as an attachment and later retrieve the attachments containing the data to get the original file back. To keep track of our file, we'll store the message IDs in the local database which doesn't require much space. The metadata itself takes up very little storage.

The issue is that Discord has an upload limit of only 8-megabyte which means that we can't store a file which exceeds 8-megabyte.

To address this issue, there are two approaches:

1. Dev Detour's approach

2. My approach, which is more efficient.

## First approach (Dev Detour's approach, A classic one)

The first approach suggests us to take a large file, split it into smaller 8-megabyte chunks, and upload them to a Discord channel. We will use message IDs to track these chunks, which will be stored in a local database. 

Whenever we need to download the original file, we can simply fetch the chunks using stored message IDs, combine those chunks, and save the original file locally.

For example, if we want to upload a 32-megabyte file, we will split the file into 8-megabyte chunks. In this case, we would have four chunks. To upload them, we have to send four http requests. Each chunk will be tracked by a message ID.

So, for a 32-megabyte file, four message IDs will be stored. The metadata we store for this approach would look something like this:

```json
[
    {
        "file_name": "my_32_megabytes_file.bin",
        "size": "33554432", // the size will be in bytes
        "uploaded_at": "DATE_UPLOADED",
        "data_ref": [/* list of message ids*/]
    },
    {
        "file_name": "my_64_megabytes_file.bin",
        "size": "67108864", // the size will be in bytes
        "uploaded_at": "DATE_UPLOADED",
        "data_ref": [/* list of message ids*/]
    }
]
```
With this approach, we can store very huge files and there's no limit. It can go upto 1 tb.


The issue with the this approach is that it is designed to handle only one file at a time. What if we want to upload a folder containing hundereds of files? If we rely on this approach, we may have to send hundereds of requests just to upload a small amount of data. Which is very inefficient.

For example, let's say I have a folder containing 200 files where the size of each file is 2-kilobyte. The total folder size would be 400 kilobytes. Using the this approach, we would have to send 200 requests just to upload 400 kilobytes of data. This is not only inefficient but could also abuse the API.


## Approach Two (My approach, Enchanced one)

Approach two suggests us to treat an attachment not as an 8-megabyte chunk of a file, but as a memory cell. This 8-megabyte memory cell can hold multiple files as well as chunks of different files.

Since Discord allows us to upload 10 attachments per message, each upto 8 megabytes in size, we can upload 80 megabytes of data with just one message request. Therefore, each message will contain 10 memory cells.

For example, if we have a folder containing 100 files, each 2 kilobytes in size, we can store all 100 files in just a single memory cell. As a result, we only have to send one HTTP request.

Now what if we want to download a single 2-kilobyte file? Do we have to fetch a whole memory cell or in this case, an attachment containing 200 kilobytes of data? Thankfully no. We can track the position of current file's data in the memory cell and use the **Range** header to fetch only the required data from an attachment or memory cell.

With this approach, we can efficiently upload not only large files, but also folders containing hundereds of files with minimal http requests. Because we are treating each attachment as a memory cell and each message as a batch containing 10 cells.

The metadata for this approach looks something like this:

**Example one**

```json
{
    "root": {
        "total_storage_used": 11534336, // size in bytes
        "children": [
            "id-1",
            "id-2"
        ]
    },
    "id-1": {
        "parent_id": "root",
        "id": "id-1",
        "size": 1048576, // size in bytes
        "name": "PDF FILES",
        "type": "DIRECTORY",
        "uploaded_at": "DATE_UPLOADED",
        "children": [
            "id-3"
        ]
    },
    "id-2": {
        "parent_id": "root",
        "id": "id-2",
        "size": 10485760, // size in bytes
        "name": "app.exe",
        "type": "FILE",
        "uploaded_at": "DATE_UPLOADED",  
        "data_ref": {
            "message-id-two": [
                {
                    "cell_index": 0,
                    "position": {
                        "start_position": 0,
                        "end_position": 8388608
                    },
                    "cell_index": 1,
                    "position": {
                        "start_position": 0,
                        "end_position": 2097152 // remaining two megabytes in the second cell
                    }
                }
            ]
        }
    },
    "id-3": {
        "parent_id": "id-1",
        "id": "id-3",
        "size": 1048576, // size in bytes
        "name": "resume.pdf",
        "type": "FILE",
        "uploaded_at": "DATE_UPLOADED",  
        "data_ref": {
            "message-id-one": [
                {
                    "cell_index": 0,
                    "position": {
                        "start_position": 0,
                        "end_position": 1048576
                    }
                }
            ]
        }
    }
}
```

**Example two**

```json
{
    "root": {
        "total_storage_used": 6291456, // size in bytes
        "children": [
            "id-1",
        ]
    },
    "id-1": {
        "parent_id": "root",
        "id": "id-1",
        "size": 6291456, // size in bytes
        "name": "MP3 FILES",
        "type": "DIRECTORY",
        "uploaded_at": "DATE_UPLOADED",
        "children": [
            "id-2",
            "id-3",
        ]
    },
    "id-2": {
        "parent_id": "id-1",
        "id": "id-2",
        "size": 3145728, // size in bytes
        "name": "one.mp3",
        "type": "FILE",
        "uploaded_at": "DATE_UPLOADED",  
        "data_ref": {
            "message-id-one": [
                {
                    "cell_index": 0,
                    "position": {
                        "start_position": 0,
                        "end_position": 3145728
                    }
                }
            ]
        }
    },
    "id-3": {
        "parent_id": "id-1",
        "id": "id-3",
        "size": 3145728, // size in bytes
        "name": "two.mp3",
        "type": "FILE",
        "uploaded_at": "DATE_UPLOADED",  
        "data_ref": {
            "message-id-one": [
                {
                    "cell_index": 0,
                    "position": {
                        "start_position": 3145728,
                        "end_position": 6291456
                    }
                }
            ]
            // Yup, we will use the same message id and the same cell because we have some space :D
        }
    }
}
```

## Explanation

The **data_ref** helps us to track the exact location of our file. It contains the actual message ID as a key and list of **cells** as a value. There can be multiple message IDs in **data_ref**, depending upon the size of the file. Each list of cells can contain maximum 10 cells.

The **id-1**, **id-2** is used here as an example. We will actually generate the IDs using: 
```kotlin
import java.util.UUID

UUID.randomUUID().toString()
```

This tool uses a discord bot to upload the data. I've tried using the webhook but I was not able to upload 10 attachments in one message.


# All Seven Commands

**Upload (upload)**

The upload command is used to upload a file or a folder. IT takes a path to a file or a folder and upload it on discord.

**Download (download)**

The download command is used to download a file or a folder. It takes a number that corresponds to a file or folder that has been uploaded and then downloads the corresponding file or folder.

**Open (open)**

The open command is used to upload a folder. It takes a number that corresponds to a folder that has been uploaded.

**Delete (delete)**

The delete command is used to delete a file or a folder. It takes a number that corresponds to a file or a folder that has been uploaded. This command simply removes the metadata of the file or a folder from the database. It doesn't actually delete the data from discord.

**Back (back)**

The back command is used to return to the previous folder. You can also jump back N folders using `back N`.

**Set Token (set-token)**

The set-token command is used to set the token of a discord bot that you want to use.

**Set Channel Id (set-channel-id)**

The set-channel-id command is used to set the channel id where our data will be uploaded.

## Usage

First, click [here](https://github.com/HARUM1122/disdrive-cli/releases/tag/disdrive) and download **disdrive.rar** and extract it to a separate folder. Then launch the **app.exe** and run the **set-token** command to set the token of the discord bot you want to use. `set-token YOUR-TOKEN`

After that, create a private server and copy the id of a text channel where you want your data to be uploaded. `set-channel-id YOUR_CHANNEL_ID`

Now you can upload and download files or folders.


**If you have any questions, you can dm me on Twitter or X:**
`@HELI_FN`