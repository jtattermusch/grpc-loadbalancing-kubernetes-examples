// Copyright 2015 gRPC authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

using System;
using System.Collections.Generic;
using Grpc.Core;
using Helloworld;
using System.Threading;

namespace GreeterClient
{
    class Program
    {
        public static void Greet()
        {
            var channelOptions = new List<ChannelOption> {};
            var lbPolicyName = Environment.GetEnvironmentVariable("GREETER_LB_POLICY_NAME");
            if (!string.IsNullOrEmpty(lbPolicyName))
            {
                Console.WriteLine("Will use " + lbPolicyName + " load balancing policy");
                channelOptions.Add(new ChannelOption("grpc.lb_policy_name", lbPolicyName));
            }

            var channelTarget = Environment.GetEnvironmentVariable("GREETER_SERVICE_TARGET");
            Console.WriteLine("Creating channel with target " + channelTarget);

            // Resolve backend IP using cluster-internal DNS name of the backend service
            Channel channel = new Channel(channelTarget, ChannelCredentials.Insecure, channelOptions);

            var client = new Greeter.GreeterClient(channel);
            String user = "you";
            
            for (int i = 0; i < 10000; i++)
            {
                try {
                  var reply = client.SayHello(new HelloRequest { Name = user });
                  Console.WriteLine("Greeting: " + reply.Message);
                } 
                catch (RpcException e)
                {
                   Console.WriteLine("Error invoking greeting: " + e.Status);
                }
                
                Thread.Sleep(1000);
            }
            channel.ShutdownAsync().Wait();
            Console.WriteLine();
        }
        public static void Main(string[] args)
        {
            Greet();
        }
    }
}
