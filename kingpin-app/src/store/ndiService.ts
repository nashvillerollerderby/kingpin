import { createApi } from "@reduxjs/toolkit/query/react";
import { fetchBaseQuery } from "@reduxjs/toolkit/query";

export const ndiApi = createApi({
  reducerPath: 'ndiApi',
  baseQuery: fetchBaseQuery({ baseUrl: '/api/ndi/' }),
  tagTypes: ['Sources', 'SourceData', 'Running'],
  endpoints: (build) => ({
    refreshSources: build.mutation<string, void>({
      query: () => "refresh",
      invalidatesTags: ['Sources'],
    }),
    getSources: build.query<string[], void>({
      query: () => "sources",
      transformResponse: (response: { sources: string[] }) => response.sources,
      providesTags: () => [{ type: 'Sources', id: 'LIST' }],
    }),
    isRunning: build.query<boolean, void>({
      query: () => "running",
      providesTags: () => [{ type: 'Running', id: 'STREAM' }],
    }),
    stop: build.mutation<string, void>({
      query: () => "stop",
      invalidatesTags: ['Running'],
    }),
    selectSource: build.mutation<string, string>({
      query: source => `select-source/${source}`,
      invalidatesTags: ['SourceData', 'Running'],
    })
  }),
})

/**
 *         .route("/refresh", get(refresh_sources))
 *         .route("/sources", get(get_sources))
 *         .route("/running", get(is_running))
 *         .route("/select-source/{name}", get(select_source))
 *         .route("/ptz", get(ptz).post(ptz_control))
 *         .route("/stop", get(stop_stream))
 */

// Export hooks for usage in functional components, which are
// auto-generated based on the defined endpoints
export const { useRefreshSourcesMutation, useGetSourcesQuery, useIsRunningQuery, useStopMutation, useSelectSourceMutation } = ndiApi