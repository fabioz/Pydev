..
    <right_area>
    <p>Getting started with PyDev!</p>
    </right_area>
    
    
    <image_area>manual.png</image_area>
    
    
    <quote_area><strong>PyDev 101</strong></quote_area>
    
    <root>manual_101_root</root>


Configuring Eclipse to suit your needs
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Well, Eclipse has lots of things you can configure, and below are some
things that really make a difference...

-- actually, there is only 1 tip currently, but I intend to make it
grow with the questions I receive about it, now that I have a good place
to put those ;-)

-  **Configuring auto-refresh**: Eclipse by default (at least on 3.1)
   does not refresh things automatically, so, if you make changes
   outside of the workspace, you will not see the changes until you
   refresh it (F5 on the navigator). However, you can change the default
   setting and ask Eclipse to refresh automatically.
   
   To set auto-refresh, go to **window > preferences > general >
   workspace** and check the **refresh automatically** check-box.
   **NOTE:** not doing so may have some specially strange results with
   .pyc files not being deleted, as PyDev will only acknowledge that the
   .pyc file exists on a refresh.


