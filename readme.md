# 環境
* openjdk : 1.8.0_252
# Server
* Using Thread handle multi-client
* 只能同時處理兩個 client
* 虛擬碼
    ```
    open server's window
    thread
        accept
        recieve client's name
        if (two client connected)
            while (no result)
                battle
    ```
* 自行關閉 server 會導致非預期錯誤
* Client 端關閉之後, Server 的畫面還會留著, 但不具備任何功能, 需手動關閉再重啟才能重新接收 client
# Client
* 使用 JFrame, JPanel, JLabel...等設計 UI
* 執行後輸入名稱並按下 start 即可進入遊戲
* 虛擬碼
    ```
    connect to server
    input user name
    send user name to server
    in waiting room
    battle start
    ```
* 遊戲分出勝負後會自行關閉

# 免編譯直接執行
* 執行 myServer.jar 以及 myClient.jar 即可
