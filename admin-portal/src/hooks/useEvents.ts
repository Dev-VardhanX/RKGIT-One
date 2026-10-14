import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";

import {
  createEvent,
  deleteEvent,
  getEvent,
  getEvents,
  updateEvent,
  type EventInput,
} from "@/lib/events";

export const EVENTS_QUERY_KEY = ["events"];

export function useEvents() {
  return useQuery({
    queryKey: EVENTS_QUERY_KEY,
    queryFn: getEvents,
  });
}

export function useEvent(id: string) {
  return useQuery({
    queryKey: [...EVENTS_QUERY_KEY, id],
    queryFn: () => getEvent(id),
    enabled: Boolean(id),
  });
}

export function useCreateEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: EventInput) => createEvent(input),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: EVENTS_QUERY_KEY,
      });
    },
  });
}

export function useUpdateEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      input,
      previousPosterPath,
    }: {
      id: string;
      input: EventInput;
      previousPosterPath: string | null;
    }) => updateEvent(id, input, previousPosterPath),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: EVENTS_QUERY_KEY,
      });
    },
  });
}

export function useDeleteEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      posterPath,
    }: {
      id: string;
      posterPath: string | null;
    }) => deleteEvent(id, posterPath),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: EVENTS_QUERY_KEY,
      });
    },
  });
}