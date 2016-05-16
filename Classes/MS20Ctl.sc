// ===========================================================================
// Title         : MS20Ctl
// Description   : Interface class for Korg MS-20 MIDI controller
// Version       : 0.1
// Copyright (c) : David Granström 2016
// ===========================================================================

MS20Ctl {
    var ctls;

    *new {
        ^super.new.init;
    }

    init {
        MIDIClient.init;
        MIDIIn.connectAll;
        // MIDIIn.findPort("MS-20 Controller", "IN");

        ctls = ();
        ctls.patch = MS20PatchBay();
    }

    doesNotUnderstand {|selector ... args|
        ^ctls[selector] ?? { ^super.doesNotUnderstand(selector, args) }
    }
}

MS20PatchBay {
    var actions, ctls;
    var lastConnection;

    *new {|key|
        ^super.new.initPatchBay;
    }

    initPatchBay {
        var packetList = Array.new(3);

        ctls = (
            0: \total,
            19: \pink,
            23: \white,
            29: \signalOut
        );

        actions = ();
        actions[\connect] = ();
        actions[\disconnect] = ();
        actions[\patch] = ();

        MIDIdef.cc(\io, {|msg|
            packetList = packetList.add(msg);
            // collect 3 consecutive cc messages
            if(packetList[2].notNil) {
                this.parse(packetList);
                packetList = Array.new(3);
            }
        }, [ 99, 98, 6 ]); // event, param, echo
    }

    onConnect {|parameter, func|
        actions.connect[parameter] = func;
    }

    onDisconnect {|parameter, func|
        actions.disconnect[parameter] = func;
    }

    onPatch {|... args|
        var addr1, addr2, connectFunc, disconnectFunc;
        #addr1, addr2, connectFunc, disconnectFunc = args;

        actions[\patch][addr1] ?? {
            actions[\patch][addr1] = ();
        };

        actions[\patch][addr1][addr2] = connectFunc;
    }

    parse {|message|
        var from, to;
        var event, addr1, addr2;
        #event, addr1, addr2 = message;

        from = ctls[addr1];
        to = ctls[addr2];

        message.debug('message');

        switch (event)
        { 4 } {
            if(from == to) { // just one connection
                // execute connect callback
                actions[\connect][from].value;
            } {
                if(from != to) {
                    // execute connect callback
                    "patch from: % to: %\n".postf(from, to);
                }
            }
        }
        { 5 } {
            // "disconnect".postln;
            // param.postln;
            if(from == to) {
                actions[\disconnect][from].value;
            }
        };
    }
}
