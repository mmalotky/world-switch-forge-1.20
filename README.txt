World Switch
----------------
Adds "world" command to Minecraft Servers to save and switch worlds between restarts, all while saving player
data between worlds resets.

Syntax: world [action] [world]

Actions:
set -  Specifies the world to be run when the server restarts to the worldConfig.cfg file and shuts down the server.
       Specifying "new" will set the server to create a new world during the server restart.
save - Saves the current world to the worlds folder under the specified name. Need not be employed to update worlds
       currently saved in the worlds folder if the world is running via by the set command.