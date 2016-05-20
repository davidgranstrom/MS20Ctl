// ===========================================================================
// Title         : MS20Ctl
// Description   : Interface class for Korg MS-20 MIDI controller
// Version       : 0.1
// Copyright (c) : David Granström 2016
// ===========================================================================

MS20Ctl : MS20PatchBay {
    var ctls;

    *new {
        ^super.new.initMS20Ctl;
    }

    initMS20Ctl {
        var knobs;

        MIDIClient.init;
        MIDIIn.connectAll;
        // MIDIIn.findPort("MS-20 Controller", "IN");

        ctls = ();
        knobs = ();

        // knob cc nums
        knobs['vcf highpass'] = 28;
        knobs['vcf lowpass'] = 74;
        knobs['vco1 scale'] = 15;
        knobs['vco2 waveform'] = 82;
        knobs['vco2 pitch'] = 19;
        knobs['vco2 scale'] = 22;
        knobs['vco portamento'] = 5;
        knobs['vco master tune'] = 18;
        knobs['vco freq mod mg/t.ext'] = 12;
        knobs['vco freq mod eg1/ext'] = 93;
        knobs['vco mixer vco1 level'] = 20;
        knobs['vco mixer vco2 level'] = 21;
        knobs['vcf highpass cutoff'] = 28;
        knobs['vcf highpass peak'] = 29;
        knobs['vcf highpass cutoff mod MG/T.EXT'] = 30;
        knobs['vcf highpass cutoff mod EG2/EXT'] = 31;
        knobs['vcf lowpass cutoff'] = 74;
        knobs['vcf lowpass peak'] = 71;
        knobs['vcf lowpass cutoff mod MG/T.EXT'] = 85;
        knobs['vcf lowpass cutoff mod EG2/EXT'] = 79;
        knobs['mod gen wave form'] = 76;
        knobs['mod gen freq'] = 27;
        knobs['eg1 delay time'] = 24;
        knobs['eg1 attack time'] = 23;
        knobs['eg1 release time'] = 26;
        knobs['eg2 hold time'] = 25;
        knobs['eg2 attack time'] = 73;
        knobs['eg2 decay time'] = 75;
        knobs['eg2 sustain level'] = 23;
        knobs['eg2 release time'] = 72;
        knobs['signal level'] = 11;
        knobs['low cut freq'] = 88;
        knobs['high cut freq'] = 89;
        knobs['cv adjust'] = 90;
        knobs['threshold level'] = 91;
        knobs['volume'] = 7;

        // assign responders
        knobs.keysValuesDo {|key, cc|
            var ctl = MS20Knob(key, cc);
            ctls.put(key, ctl);
        };
    }

    onChange {|param, func|
        ctls[param].onChange_(func);
    }
}

MS20Knob {
    var key, cc;

    *new {|key, cc|
        ^super.newCopyArgs(("ms20_" ++ key).asSymbol, cc);
    }

    onChange_ {|func|
        MIDIdef.cc(key, func, cc);
    }

    free {
        MIDIdef.cc(key).free;
    }
}

MS20PatchBay {
    var actions, inputs;
    var lastConnection;

    *new {
        ^super.new.initPatchBay;
    }

    initPatchBay {
        var packetList = Array.newClear(3);
        var idx = 0;

        // patch bay inputs
        inputs = (
            0:  'total',
            6:  'mod gen out saw',
            2:  'sample and hold in',
            3:  'amp signal in',
            5:  'freq',
            4:  'clock',
            6:  'mod gen out square',
            7:  'sample and hold out',
            8:  'amp out',
            9:  'hp filter ext signal in',
            10: 'envelope generator out',
            11: 'rev out',
            12: 'vca in',
            13: 'hp filter cutoff',
            14: 'vca control input',
            16: 'vca out',
            15: 'bpf out',
            17: 'lpf cutoff',
            18: 'eg1 trig in',
            19: 'pink',
            20: 'fv cv out',
            21: 'amp initial gain',
            22: 'eg2 rev out',
            23: 'white',
            24: 'vco2 cv in',
            25: 'kbd cv in',
            26: 'kbd trig in',
            27: 'half circle symbol',
            28: 'env out',
            29: 'signal out',
            30: 'phones',
            31: 'kbd cv out',
            32: 'kbd trig out',
            33: 'square symbol',
            34: 'trig out'
        );

        // action types
        actions = ();
        actions.patch = ();
        actions.single = ();

        // storage for connect/disconnect callbacks
        actions.single.connect = ();
        actions.single.disconnect = ();
        actions.patch.connect = ();
        actions.patch.disconnect = ();

        MIDIdef.cc(\ms20_patchbay_io, {|msg|
            packetList[idx] = msg;
            idx = idx + 1;
            // collect 3 consecutive cc messages
            if(idx == 3) {
                this.parse(packetList);
                idx = 0;
            }
        }, [ 99, 98, 6 ]); // event, addr1, addr2
    }

    onConnect {|parameter, func|
        actions.single.connect[parameter] = func;
    }

    onDisconnect {|parameter, func|
        actions.single.disconnect[parameter] = func;
    }

    onPatch {|addr1, addr2, connectFunc, disconnectFunc|
        if(connectFunc.isFunction) {
            actions.patch.connect[addr1] ?? {
                actions.patch.connect[addr1] = ();
            };

            actions.patch.connect[addr1][addr2] = connectFunc;
        };

        if(disconnectFunc.isFunction) {
            actions.patch.disconnect[addr1] ?? {
                actions.patch.disconnect[addr1] = ();
            };

            actions.patch.disconnect[addr1][addr2] = disconnectFunc;
        }
    }

    parse {|message|
        var from, to, eventType;
        var event, addr1, addr2;
        #event, addr1, addr2 = message;

        from = inputs[addr1];
        to = inputs[addr2];

        eventType = (event == 4).if(\connect, \disconnect);

        if(from == to) {
            // just one connection
            actions.single[eventType][from].value;
        } {
            // see if there is a patch callback
            actions.patch[eventType][from] !? {
                actions.patch[eventType][from][to].value;
            }
        };
    }
}
