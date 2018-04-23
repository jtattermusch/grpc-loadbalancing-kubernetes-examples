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
using Grpc.Core;
using Helloworld;
using System.Threading;

namespace GreeterClient
{
    class Program
    {
        public static void Greet()
        {
            // remove the explicit DNS server address once https://github.com/grpc/grpc/pull/15073 is merged and released.
            Channel channel = new Channel("dns://10.3.240.10:53/greeter-server-loadbalanced.default.svc.cluster.local:8000", ChannelCredentials.Insecure);

            Console.WriteLine("Created new channel, target: " + channel.Target);

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
